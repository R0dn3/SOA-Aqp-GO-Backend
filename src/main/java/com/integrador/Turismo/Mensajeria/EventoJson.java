package com.integrador.Turismo.Mensajeria;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte el evento a texto JSON (legible en el panel de RabbitMQ) y de
 * vuelta.
 * Son solo 6 campos de texto, así que no hace falta una librería.
 */
public final class EventoJson {

    private EventoJson() {
    }

    public static String escribir(EventoReserva e) {
        return "{"
                + campo("tipo", e.tipo() == null ? null : e.tipo().name()) + ","
                + campo("reservaId", e.reservaId()) + ","
                + campo("usuarioId", e.usuarioId()) + ","
                + campo("estadoPago", e.estadoPago()) + ","
                + campo("correlationId", e.correlationId()) + ","
                + campo("creadoEn", e.creadoEn() == null ? null : e.creadoEn().toString())
                + "}";
    }

    public static EventoReserva leer(String json) {
        String tipo = valor(json, "tipo");
        String creado = valor(json, "creadoEn");
        return new EventoReserva(
                EventoReserva.Tipo.valueOf(tipo),
                valor(json, "reservaId"),
                valor(json, "usuarioId"),
                valor(json, "estadoPago"),
                valor(json, "correlationId"),
                creado == null ? null : LocalDateTime.parse(creado));
    }

    private static String campo(String nombre, String valor) {
        if (valor == null) {
            return "\"" + nombre + "\":null";
        }
        return "\"" + nombre + "\":\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String valor(String json, String nombre) {
        Matcher m = Pattern
                .compile("\"" + nombre + "\"\\s*:\\s*(null|\"((?:[^\"\\\\]|\\\\.)*)\")")
                .matcher(json);
        if (!m.find() || m.group(2) == null) {
            return null;
        }
        return m.group(2).replace("\\\"", "\"").replace("\\\\", "\\");
    }
}