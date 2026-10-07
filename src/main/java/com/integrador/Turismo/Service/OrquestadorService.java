package com.integrador.Turismo.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.integrador.Turismo.DTO.PagoRequest;
import com.integrador.Turismo.DTO.PagoResponse;
import com.integrador.Turismo.DTO.ReservaRequest;
import com.integrador.Turismo.DTO.ReservaResponse;
import com.integrador.Turismo.Model.Pago;
import com.integrador.Turismo.Model.Reserva;
import com.integrador.Turismo.Soap.ReservarYPagarRequest;
import com.integrador.Turismo.Soap.ReservarYPagarResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orquestador de la composición: Crear reserva -> Cobrar pago -> Confirmar.
 * Invoca ReservaService y PagoService en secuencia; si el pago es rechazado
 * (o si los datos de pago vienen mal formados), compensa cancelando la
 * reserva recién creada en vez de dejarla huérfana en PENDIENTE_PAGO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrquestadorService {

    private final ReservaService reservaService;
    private final PagoService pagoService;

    @Transactional
    public ReservarYPagarResponse reservarYPagar(ReservarYPagarRequest req) {

        // Paso 1: Crear reserva (delega en ReservaService)
        ReservaRequest reservaReq = new ReservaRequest(
                req.getPaqueteId(),
                LocalDate.parse(req.getFechaSalida()),
                req.getNumPersonas(),
                null // esta operación no maneja acompañantes, igual que crearReserva SOAP
        );

        ReservaResponse reserva = reservaService.crear(reservaReq, req.getUsuarioId());
        log.info("[Orquestador] Reserva creada: {}", reserva.id());

        // Paso 2: Cobrar pago (delega en PagoService)
        // Todo este bloque va dentro del try: si "monto" o "metodo" vienen
        // mal formados, también debe compensarse la reserva ya creada.
        PagoResponse pago;
        try {
            PagoRequest pagoReq = new PagoRequest(
                    reserva.id(),
                    new BigDecimal(req.getMonto()),
                    Pago.Metodo.valueOf(req.getMetodo()),
                    req.getReferencia());
            pago = pagoService.procesarPago(pagoReq);
        } catch (RuntimeException ex) {
            log.warn("[Orquestador] Pago falló con excepción, compensando reserva {}", reserva.id());
            reservaService.cambiarEstado(reserva.id(), Reserva.Estado.CANCELADA);
            throw ex;
        }

        // Paso 3: Confirmar o compensar según el resultado del pago
        String mensaje;
        String estadoFinal;

        if ("VERIFICADO".equals(pago.estado())) {
            estadoFinal = Reserva.Estado.CONFIRMADA.name();
            mensaje = "Pago verificado y reserva confirmada";
        } else {
            reservaService.cambiarEstado(reserva.id(), Reserva.Estado.CANCELADA);
            estadoFinal = Reserva.Estado.CANCELADA.name();
            mensaje = "Pago rechazado: " + pago.motivoRechazo() + ". Reserva cancelada automáticamente";
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
}