package com.integrador.Turismo.DTO;

import com.integrador.Turismo.Model.Ticket;

import java.time.LocalDateTime;

public record TicketResumenDto(
        String         id,
        String         asunto,
        String         tipo,
        String         estado,
        String         prioridad,
        String         usuarioNombre,
        String         usuarioEmail,
        String         reservaId,       // null si no tiene reserva
        String         adminNombre,     // null si no está asignado
        LocalDateTime  createdAt,
        LocalDateTime  updatedAt,
        int            totalMensajes
) {

    public static TicketResumenDto from(Ticket t) {
        return new TicketResumenDto(
                t.getId(),
                t.getAsunto(),
                t.getTipo().name(),
                t.getEstado().name(),
                t.getPrioridad().name(),
                t.getUsuario().getNombreCompleto(),
                t.getUsuario().getEmail(),
                t.getReserva() != null ? t.getReserva().getId() : null,
                t.getAdminAsignado() != null ? t.getAdminAsignado().getNombreCompleto() : null,
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getMensajes().size()
        );
    }
}
