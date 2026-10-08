package com.integrador.Turismo.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.integrador.Turismo.DTO.AcompananteDto;
import com.integrador.Turismo.DTO.PagoRequest;
import com.integrador.Turismo.DTO.PagoResponse;
import com.integrador.Turismo.DTO.ReservaRequest;
import com.integrador.Turismo.DTO.ReservaResponse;
import com.integrador.Turismo.Model.Pago;
import com.integrador.Turismo.Model.Reserva;
import com.integrador.Turismo.Soap.AcompananteOrqItem;
import com.integrador.Turismo.Soap.ReservarYPagarRequest;
import com.integrador.Turismo.Soap.ReservarYPagarResponse;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orquestador de la composición: Crear reserva -> Cobrar pago -> Confirmar.
 * Invoca ReservaService y PagoService en secuencia; si el pago es rechazado
 * (o falla técnicamente), compensa cancelando la reserva recién creada.
 *
 * Las etiquetas [ESB-...] marcan los pasos del patrón VETRO
 * (Validate-Enrich-Transform-Route-Operate) y los patrones de mediación
 * (Splitter, Content Enricher, Content-Based Router) aplicados de forma
 * embebida. Los mensajes rechazados se guardan en dead-letter [ESB-DLQ] y
 * cada resultado se cuenta en métricas (aqpgo.esb.mensajes).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrquestadorService {

    private static final String OPERACION = "reservarYPagar";

    private final ReservaService reservaService;
    private final PagoService pagoService;
    private final DeadLetterService deadLetterService;
    private final MeterRegistry meterRegistry;

    /** Datos ya validados y convertidos a sus tipos definitivos. */
    private record DatosValidados(LocalDate fechaSalida, BigDecimal monto, Pago.Metodo metodo,
            List<AcompananteDto> acompanantes) {
    }

    @Transactional
    public ReservarYPagarResponse reservarYPagar(ReservarYPagarRequest req) {
        Timer.Sample sample = Timer.start(meterRegistry);
        contar("recibido");
        try {
            return ejecutar(req);
        } finally {
            sample.stop(meterRegistry.timer("aqpgo.esb.orquestacion.duracion", "operacion", OPERACION));
        }
    }

    private ReservarYPagarResponse ejecutar(ReservarYPagarRequest req) {

        // [ESB-VALIDATE] Se valida lo más temprano posible, ANTES de invocar a
        // ningún servicio miembro: un mensaje inválido no crea nada.
        DatosValidados datos;
        try {
            datos = validar(req);
        } catch (IllegalArgumentException ex) {
            contar("rechazado_validacion");
            registrarDeadLetter("VALIDACION", ex.getMessage(), req);
            throw ex;
        }
        log.info("[ESB-VALIDATE] Mensaje válido (formato y reglas básicas). usuarioId={}, paqueteId={}",
                req.getUsuarioId(), req.getPaqueteId());

        // [ESB-SPLIT] El mensaje compuesto se divide; primer sub-mensaje:
        // ReservaRequest (incluye los acompañantes).
        log.info(
                "[ESB-SPLIT] Mensaje compuesto dividido: extrayendo sub-mensaje ReservaRequest (paquete, fecha, nº personas, {} acompañante(s))",
                datos.acompanantes().size());

        ReservaRequest reservaReq = new ReservaRequest(
                req.getPaqueteId(),
                datos.fechaSalida(),
                req.getNumPersonas(),
                datos.acompanantes());

        // Paso 1: Crear reserva (delega en ReservaService)
        ReservaResponse reserva = reservaService.crear(reservaReq, req.getUsuarioId());
        log.info("[Orquestador] Reserva creada: {}", reserva.id());

        // [ESB-SPLIT] segundo sub-mensaje + [ESB-ENRICH]: se añade el reservaId,
        // dato que el cliente nunca envió.
        log.info("[ESB-SPLIT] Extrayendo sub-mensaje PagoRequest (monto, metodo, referencia)");
        log.info(
                "[ESB-ENRICH] PagoRequest enriquecido con reservaId={} (dato no provisto por el cliente, obtenido del paso anterior)",
                reserva.id());

        // Paso 2: Cobrar pago (delega en PagoService)
        PagoResponse pago;
        try {
            PagoRequest pagoReq = new PagoRequest(
                    reserva.id(),
                    datos.monto(),
                    datos.metodo(),
                    req.getReferencia());

            // [ESB-OPERATE] Invoca al servicio miembro, con manejo de error.
            log.info("[ESB-OPERATE] Invocando PagoService.procesarPago(reservaId={}, monto={}, metodo={})",
                    reserva.id(), datos.monto(), datos.metodo());
            pago = pagoService.procesarPago(pagoReq);
        } catch (RuntimeException ex) {
            log.warn("[ESB-OPERATE] Fallo técnico en PagoService ({}). Disparando compensación.", ex.getMessage());
            log.warn("[Orquestador] Pago falló con excepción, compensando reserva {}", reserva.id());
            contar("fallo_tecnico");
            registrarDeadLetter("FALLO_TECNICO", ex.getMessage(), req);
            reservaService.cambiarEstado(reserva.id(), Reserva.Estado.CANCELADA);
            throw ex;
        }

        // [ESB-ROUTE] Content-Based Router: el destino del flujo depende del
        // contenido de la respuesta de pago.
        log.info("[ESB-ROUTE] Ruteando según contenido de la respuesta: estadoPago={}", pago.estado());

        // Paso 3: Confirmar o compensar según el resultado del pago
        String mensaje;
        String estadoFinal;

        if ("VERIFICADO".equals(pago.estado())) {
            estadoFinal = Reserva.Estado.CONFIRMADA.name();
            mensaje = "Pago verificado y reserva confirmada";
            contar("confirmado");
            log.info("[ESB-ROUTE] -> rama CONFIRMAR. Reserva {} pasa a CONFIRMADA", reserva.id());
        } else {
            reservaService.cambiarEstado(reserva.id(), Reserva.Estado.CANCELADA);
            estadoFinal = Reserva.Estado.CANCELADA.name();
            mensaje = "Pago rechazado: " + pago.motivoRechazo() + ". Reserva cancelada automáticamente";
            contar("compensado");
            log.info("[ESB-ROUTE] -> rama COMPENSAR. Motivo: {}", pago.motivoRechazo());
            log.info("[Orquestador] Pago rechazado, reserva {} compensada (CANCELADA)", reserva.id());
        }

        ReservarYPagarResponse response = new ReservarYPagarResponse();
        response.setReservaId(reserva.id());
        response.setEstadoReserva(estadoFinal);
        response.setPagoId(pago.id());
        response.setEstadoPago(pago.estado());
        response.setMensaje(mensaje);
        return response;
    }

    /** [ESB-VALIDATE] Valida y convierte los campos del mensaje compuesto. */
    private DatosValidados validar(ReservarYPagarRequest req) {

        if (req.getPaqueteId() == null || req.getPaqueteId().isBlank())
            throw rechazo("paqueteId es obligatorio");

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(req.getFechaSalida());
        } catch (RuntimeException e) {
            throw rechazo("fechaSalida debe tener formato AAAA-MM-DD");
        }
        if (!fecha.isAfter(LocalDate.now()))
            throw rechazo("fechaSalida debe ser una fecha futura");

        if (req.getNumPersonas() < 1)
            throw rechazo("numPersonas debe ser al menos 1");

        // Acompañantes: el titular cuenta como una persona, por eso el máximo es
        // numPersonas - 1.
        List<AcompananteOrqItem> items = req.getAcompanante();
        if (items.size() > req.getNumPersonas() - 1)
            throw rechazo("acompañantes (" + items.size() + ") no puede superar numPersonas - 1 ("
                    + (req.getNumPersonas() - 1) + ")");

        List<AcompananteDto> acompanantes = new ArrayList<>();
        int n = 0;
        for (AcompananteOrqItem a : items) {
            n++;
            if (a.getNombreCompleto() == null || a.getNombreCompleto().isBlank())
                throw rechazo("acompañante " + n + ": nombreCompleto es obligatorio");
            LocalDate nacimiento = null;
            if (a.getFechaNacimiento() != null && !a.getFechaNacimiento().isBlank()) {
                try {
                    nacimiento = LocalDate.parse(a.getFechaNacimiento().trim());
                } catch (RuntimeException e) {
                    throw rechazo("acompañante " + n + ": fechaNacimiento debe tener formato AAAA-MM-DD");
                }
            }
            acompanantes.add(new AcompananteDto(
                    a.getNombreCompleto(),
                    a.getDniPasaporte(),
                    a.getPais(),
                    nacimiento,
                    a.getGenero(),
                    a.getDatosAdicionales()));
        }

        BigDecimal monto;
        try {
            monto = new BigDecimal(req.getMonto().trim());
        } catch (RuntimeException e) {
            throw rechazo("monto debe ser un número válido");
        }
        if (monto.signum() <= 0)
            throw rechazo("monto debe ser mayor que cero");

        Pago.Metodo metodo;
        try {
            metodo = Pago.Metodo.valueOf(req.getMetodo());
        } catch (RuntimeException e) {
            throw rechazo("metodo no válido. Permitidos: " + Arrays.toString(Pago.Metodo.values()));
        }

        return new DatosValidados(fecha, monto, metodo, acompanantes);
    }

    private IllegalArgumentException rechazo(String detalle) {
        log.warn("[ESB-VALIDATE] Mensaje rechazado antes de invocar a los servicios: {}", detalle);
        return new IllegalArgumentException("Solicitud inválida: " + detalle);
    }

    // ── Métricas y dead-letter ───────────────────────────────────

    private void contar(String resultado) {
        meterRegistry.counter("aqpgo.esb.mensajes",
                "operacion", OPERACION, "resultado", resultado).increment();
    }

    /**
     * Nunca debe romper el flujo principal: si falla el guardado, solo se registra.
     */
    private void registrarDeadLetter(String tipo, String motivo, ReservarYPagarRequest req) {
        try {
            String contenido = "paqueteId=" + req.getPaqueteId()
                    + ", fechaSalida=" + req.getFechaSalida()
                    + ", numPersonas=" + req.getNumPersonas()
                    + ", acompanantes=" + req.getAcompanante().size()
                    + ", monto=" + req.getMonto()
                    + ", metodo=" + req.getMetodo()
                    + ", referencia=" + req.getReferencia();
            deadLetterService.registrar(OPERACION, tipo, motivo, contenido, req.getUsuarioId());
        } catch (RuntimeException e) {
            log.error("[ESB-DLQ] No se pudo guardar el mensaje en dead-letter: {}", e.getMessage());
        }
    }
}