package com.integrador.Turismo.Mensajeria;

import java.time.LocalDateTime;

/**
 * Mensaje que viaja por la cola del ESB cuando una reserva queda confirmada o
 * cancelada.
 */
public record EventoReserva(
        Tipo tipo,
        String reservaId,
        String usuarioId,
        String estadoPago,
        String correlationId,
        LocalDateTime creadoEn) {

    public enum Tipo {
        RESERVA_CONFIRMADA,
        RESERVA_CANCELADA
    }
}