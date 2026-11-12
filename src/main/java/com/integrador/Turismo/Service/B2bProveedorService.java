//Service\B2bProveedorService.java
package com.integrador.Turismo.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.integrador.Turismo.Model.OrdenProveedor;
import com.integrador.Turismo.Repository.OrdenProveedorRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Lado del proveedor en la integración B2B (su "portal"): recibe documentos
 * ORDERS (orden de servicio) y ORDCHG (anulación) de AQP Go y responde con un
 * ORDRSP. Controles: token (401), documento válido (422), idempotencia (409) y
 * disponibilidad (503, apagable para la demo: global o por proveedor).
 */
@Service
@Slf4j
public class B2bProveedorService {

    public record SolicitudB2b(String tipoDocumento, String idDocumento, String reservaId,
            String proveedorId, String proveedorNombre, String tipoProveedor, String rol,
            String paquete, String cliente, String fechaSalida, Integer personas,
            String correlationId) {
    }

    public record RespuestaB2b(String tipoDocumento, String idDocumento, String estado, String motivo) {
    }

    public record Resultado(int status, RespuestaB2b cuerpo) {
    }

    private final OrdenProveedorRepository ordenRepository;
    private final String token;
    private final int capacidadMaxima;
    private final AtomicBoolean disponible = new AtomicBoolean(true);
    private final Set<String> caidos = ConcurrentHashMap.newKeySet(); // [POR PROVEEDOR]

    public B2bProveedorService(OrdenProveedorRepository ordenRepository,
            @Value("${aqpgo.b2b.token:TOKEN-AQPGO-2026}") String token,
            @Value("${aqpgo.b2b.capacidad-maxima:50}") int capacidadMaxima) {
        this.ordenRepository = ordenRepository;
        this.token = token;
        this.capacidadMaxima = capacidadMaxima;
    }

    public boolean isDisponible() {
        return disponible.get();
    }

    public void setDisponible(boolean valor) {
        disponible.set(valor);
        log.info("[B2B] Proveedores {}", valor ? "DISPONIBLES" : "NO DISPONIBLES (simulado)");
    }

    // [POR PROVEEDOR] Caída simulada de un proveedor concreto

    public List<String> proveedoresCaidos() {
        return List.copyOf(caidos);
    }

    public void setProveedorDisponible(String proveedorId, boolean valor) {
        if (valor) {
            caidos.remove(proveedorId);
        } else {
            caidos.add(proveedorId);
        }
        log.info("[B2B] Proveedor {} {}", proveedorId, valor ? "DISPONIBLE" : "NO DISPONIBLE (simulado)");
    }

    // ── ORDERS: orden de servicio ───────────────────────────────

    public Resultado recibirOrden(String tokenRecibido, SolicitudB2b s) {
        Resultado rechazo = controlesComunes(tokenRecibido, s);
        if (rechazo != null) {
            return rechazo;
        }

        String falta = validarOrden(s);
        if (falta != null) {
            log.warn("[B2B] 422 documento inválido: {}", falta);
            return error(422, s, falta);
        }

        String clave = s.reservaId() + ":" + s.proveedorId();
        Optional<OrdenProveedor> previa = ordenRepository.findByClaveIdempotencia(clave);
        if (previa.isPresent()) {
            log.info("[B2B] 409 orden duplicada {} (ya estaba {})", clave, previa.get().getEstado());
            return duplicado(s, previa.get());
        }

        boolean rechaza = s.personas() > capacidadMaxima;
        String estado = rechaza ? "RECHAZADA" : "ACEPTADA";
        String motivo = rechaza ? "Capacidad máxima excedida (" + capacidadMaxima + " personas)" : "Orden aceptada";

        try {
            ordenRepository.saveAndFlush(OrdenProveedor.builder()
                    .reservaId(s.reservaId())
                    .proveedorId(s.proveedorId())
                    .proveedorNombre(s.proveedorNombre())
                    .tipoProveedor(s.tipoProveedor())
                    .rol(s.rol())
                    .claveIdempotencia(clave)
                    .estado(estado)
                    .motivo(motivo)
                    .detalle(detalle(s))
                    .correlationId(s.correlationId())
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.info("[B2B] 409 orden duplicada {} (concurrente)", clave);
            return new Resultado(409, new RespuestaB2b("ORDRSP", idDoc(s), "ACEPTADA",
                    "Documento duplicado: ya fue procesado (idempotencia)"));
        }

        log.info("[B2B] ORDERS de la reserva {} para {} ({}): {}", s.reservaId(), s.proveedorNombre(), s.rol(), estado);
        return new Resultado(200, new RespuestaB2b("ORDRSP", idDoc(s), estado, motivo));
    }

    // ── ORDCHG: anulación ───────────────────────────────────────

    public Resultado recibirAnulacion(String tokenRecibido, SolicitudB2b s) {
        Resultado rechazo = controlesComunes(tokenRecibido, s);
        if (rechazo != null) {
            return rechazo;
        }
        if (vacio(s.reservaId()) || vacio(s.proveedorId())) {
            return error(422, s, "reservaId y proveedorId son obligatorios");
        }

        String clave = s.reservaId() + ":" + s.proveedorId();
        Optional<OrdenProveedor> orden = ordenRepository.findByClaveIdempotencia(clave);
        if (orden.isEmpty()) {
            return error(422, s, "No existe una orden para anular");
        }
        OrdenProveedor o = orden.get();
        if ("ANULADA".equals(o.getEstado())) {
            log.info("[B2B] 409 anulación duplicada {}", clave);
            return duplicado(s, o);
        }

        o.setEstado("ANULADA");
        o.setMotivo("Anulada por cancelación de la reserva");
        o.setActualizadaEn(LocalDateTime.now());
        ordenRepository.save(o);
        log.info("[B2B] ORDCHG: orden de la reserva {} para {} anulada", s.reservaId(), o.getProveedorNombre());
        return new Resultado(200, new RespuestaB2b("ORDRSP", idDoc(s), "ANULADA", o.getMotivo()));
    }

    // ── Utilidades ──────────────────────────────────────────────

    private Resultado controlesComunes(String tokenRecibido, SolicitudB2b s) {
        if (tokenRecibido == null || !token.equals(tokenRecibido)) {
            log.warn("[B2B] 401 token inválido o ausente");
            return error(401, s, "Token inválido o ausente");
        }
        if (!disponible.get()) {
            log.warn("[B2B] 503 proveedor no disponible (simulado)");
            return error(503, s, "Servicio del proveedor no disponible");
        }
        // [POR PROVEEDOR]
        if (s != null && s.proveedorId() != null && caidos.contains(s.proveedorId())) {
            log.warn("[B2B] 503 el proveedor {} no está disponible (simulado)", s.proveedorNombre());
            return error(503, s, "Servicio del proveedor no disponible");
        }
        return null;
    }

    private String validarOrden(SolicitudB2b s) {
        if (!"ORDERS".equals(s.tipoDocumento())) {
            return "tipoDocumento debe ser ORDERS";
        }
        if (vacio(s.reservaId())) {
            return "reservaId es obligatorio";
        }
        if (vacio(s.proveedorId())) {
            return "proveedorId es obligatorio";
        }
        if (vacio(s.rol())) {
            return "rol es obligatorio";
        }
        if (s.personas() == null || s.personas() < 1) {
            return "personas debe ser al menos 1";
        }
        return null;
    }

    private Resultado duplicado(SolicitudB2b s, OrdenProveedor previa) {
        return new Resultado(409, new RespuestaB2b("ORDRSP", idDoc(s), previa.getEstado(),
                "Documento duplicado: ya fue procesado (idempotencia)"));
    }

    private Resultado error(int status, SolicitudB2b s, String motivo) {
        return new Resultado(status, new RespuestaB2b("ERROR", idDoc(s), "ERROR", motivo));
    }

    private String idDoc(SolicitudB2b s) {
        if (s == null) {
            return null;
        }
        return !vacio(s.idDocumento()) ? s.idDocumento() : s.reservaId() + ":" + s.proveedorId();
    }

    private String detalle(SolicitudB2b s) {
        String d = (s.paquete() != null ? s.paquete() : "—") + " · " + s.personas() + " persona(s)"
                + (s.fechaSalida() != null ? " · salida " + s.fechaSalida() : "");
        return d.length() > 300 ? d.substring(0, 300) : d;
    }

    private boolean vacio(String v) {
        return v == null || v.isBlank();
    }
}