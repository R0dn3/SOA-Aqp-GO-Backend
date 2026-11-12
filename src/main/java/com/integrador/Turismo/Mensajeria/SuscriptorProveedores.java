//Mensajeria\SuscriptorProveedores.java
package com.integrador.Turismo.Mensajeria;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.integrador.Turismo.Model.OrdenProveedor;
import com.integrador.Turismo.Service.B2bDestinosService;
import com.integrador.Turismo.Service.B2bDestinosService.Destino;
import com.integrador.Turismo.Service.B2bDestinosService.ReservaB2b;

import lombok.extern.slf4j.Slf4j;

/**
 * Suscriptor 3 (coreografía + B2B): cuando una reserva se confirma, envía una
 * orden de servicio (ORDERS) a cada proveedor del paquete; si se cancela, envía
 * la anulación (ORDCHG). Es independiente de Notificaciones y Auditoría.
 * Es idempotente: si el proveedor responde 409 (ya procesado) se da por bueno,
 * así los reintentos y el reproceso nunca duplican órdenes.
 * Si un proveedor falla, se intenta igual con los demás y al final se lanza el
 * error: la cola reintenta y, si agota, queda en dead letter.
 */
@Component
@Slf4j
public class SuscriptorProveedores implements SuscriptorEsb {

    public static final String NOMBRE = "Proveedores";

    private static final String RUTA_ORDENES = "/api/b2b/proveedores/ordenes";
    private static final String RUTA_CAMBIOS = "/api/b2b/proveedores/cambios";

    private record Respuesta(int status, String cuerpo) {
    }

    private final B2bDestinosService destinos;
    private final RestClient cliente;
    private final String token;

    public SuscriptorProveedores(B2bDestinosService destinos,
            @Value("${aqpgo.b2b.url:http://localhost:${server.port:8080}}") String url,
            @Value("${aqpgo.b2b.token:TOKEN-AQPGO-2026}") String token) {
        this.destinos = destinos;
        this.token = token;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(3));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.cliente = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public boolean escucha(EventoReserva.Tipo tipo) {
        return true;
    }

    @Override
    public void procesar(EventoReserva evento) {
        if (evento.tipo() == EventoReserva.Tipo.RESERVA_CONFIRMADA) {
            enviarOrdenes(evento);
        } else {
            anularOrdenes(evento);
        }
    }

    private void enviarOrdenes(EventoReserva evento) {
        ReservaB2b reserva = destinos.cargarReserva(evento.reservaId());
        if (reserva.destinos().isEmpty()) {
            log.info("[ESB-SUBSCRIBER] [{}] La reserva {} no tiene proveedores asignados: no hay órdenes que enviar.",
                    NOMBRE, evento.reservaId());
            return;
        }
        log.info("[ESB-SUBSCRIBER] [{}] Enviando ORDERS de la reserva {} a {} proveedor(es)",
                NOMBRE, evento.reservaId(), reserva.destinos().size());

        List<String> fallos = new ArrayList<>();
        for (Destino d : reserva.destinos()) {
            try {
                Respuesta r = enviar(RUTA_ORDENES, jsonOrden(reserva, d, evento.correlationId()));
                evaluar(d.nombre(), r);
            } catch (RuntimeException e) {
                log.warn("[ESB-SUBSCRIBER] [{}] Falló el envío a {}: {}", NOMBRE, d.nombre(), e.getMessage());
                fallos.add(d.nombre());
            }
        }
        lanzarSiHuboFallos(fallos, reserva.destinos().size());
    }

    private void anularOrdenes(EventoReserva evento) {
        List<OrdenProveedor> ordenes = destinos.ordenesAceptadas(evento.reservaId());
        if (ordenes.isEmpty()) {
            log.info("[ESB-SUBSCRIBER] [{}] La reserva {} no tiene órdenes aceptadas que anular.",
                    NOMBRE, evento.reservaId());
            return;
        }
        List<String> fallos = new ArrayList<>();
        for (OrdenProveedor o : ordenes) {
            log.info("[ESB-SUBSCRIBER] [{}] Enviando ORDCHG (anulación) de la reserva {} a {}",
                    NOMBRE, evento.reservaId(), o.getProveedorNombre());
            try {
                Respuesta r = enviar(RUTA_CAMBIOS, jsonAnulacion(evento, o));
                evaluar(o.getProveedorNombre(), r);
            } catch (RuntimeException e) {
                log.warn("[ESB-SUBSCRIBER] [{}] Falló la anulación en {}: {}", NOMBRE, o.getProveedorNombre(),
                        e.getMessage());
                fallos.add(o.getProveedorNombre());
            }
        }
        lanzarSiHuboFallos(fallos, ordenes.size());
    }

    private void lanzarSiHuboFallos(List<String> fallos, int total) {
        if (!fallos.isEmpty()) {
            throw new IllegalStateException("Fallaron " + fallos.size() + " de " + total
                    + " proveedor(es): " + String.join(", ", fallos));
        }
    }

    /**
     * 200 = respuesta de negocio, 409 = ya procesado (ok); cualquier otro código es
     * error y se reintenta.
     */
    private void evaluar(String proveedor, Respuesta r) {
        if (r.status() == 200) {
            log.info("[ESB-SUBSCRIBER] [{}] {} respondió ORDRSP: {}", NOMBRE, proveedor, r.cuerpo());
        } else if (r.status() == 409) {
            log.info("[ESB-SUBSCRIBER] [{}] {} ya había procesado este documento (409, idempotente)", NOMBRE,
                    proveedor);
        } else {
            throw new IllegalStateException(
                    "Proveedor " + proveedor + " respondió HTTP " + r.status() + ": " + r.cuerpo());
        }
    }

    private Respuesta enviar(String ruta, String json) {
        return cliente.post()
                .uri(ruta)
                .header("X-B2B-Token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .exchange((peticion, respuesta) -> new Respuesta(
                        respuesta.getStatusCode().value(),
                        new String(respuesta.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }

    // ── Documentos (JSON escrito a mano) ────────────────────────

    private String jsonOrden(ReservaB2b r, Destino d, String correlationId) {
        return "{"
                + "\"tipoDocumento\":\"ORDERS\","
                + "\"idDocumento\":\"" + esc(r.reservaId() + ":" + d.proveedorId()) + "\","
                + "\"reservaId\":\"" + esc(r.reservaId()) + "\","
                + "\"proveedorId\":\"" + esc(d.proveedorId()) + "\","
                + "\"proveedorNombre\":\"" + esc(d.nombre()) + "\","
                + "\"tipoProveedor\":\"" + esc(d.tipo()) + "\","
                + "\"rol\":\"" + esc(d.rol()) + "\","
                + "\"paquete\":\"" + esc(r.paquete()) + "\","
                + "\"cliente\":\"" + esc(r.cliente()) + "\","
                + "\"fechaSalida\":\"" + esc(r.fechaSalida()) + "\","
                + "\"personas\":" + r.personas() + ","
                + "\"correlationId\":\"" + esc(correlationId) + "\""
                + "}";
    }

    private String jsonAnulacion(EventoReserva evento, OrdenProveedor o) {
        return "{"
                + "\"tipoDocumento\":\"ORDCHG\","
                + "\"idDocumento\":\"" + esc(o.getClaveIdempotencia() + ":ANULACION") + "\","
                + "\"reservaId\":\"" + esc(o.getReservaId()) + "\","
                + "\"proveedorId\":\"" + esc(o.getProveedorId()) + "\","
                + "\"correlationId\":\"" + esc(evento.correlationId()) + "\""
                + "}";
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        return b.toString();
    }
}