package com.integrador.Turismo.DTO;

import com.integrador.Turismo.Model.Ticket;
import com.integrador.Turismo.Model.TicketMensaje;

import java.time.LocalDateTime;
import java.util.List;

public record TicketDetalleDto(
        String              id,
        String              asunto,
        String              tipo,
        String              estado,
        String              prioridad,
        String              usuarioNombre,
        String              usuarioEmail,
        String              reservaId,
        String              adminNombre,
        LocalDateTime       createdAt,
        LocalDateTime       updatedAt,
        List<MensajeDto>    mensajes
) {
    public static TicketDetalleDto from(Ticket t) {
        List<MensajeDto> mensajes = t.getMensajes().stream()
                .map(MensajeDto::from)
                .toList();

        return new TicketDetalleDto(
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
                mensajes
        );
    }

    // Nested DTO para cada mensaje del hilo
    public record MensajeDto(
            String        id,
            String        autorNombre,
            String        mensaje,
            boolean       esAdmin,
            LocalDateTime createdAt
    ) {
        public static MensajeDto from(TicketMensaje m) {
            return new MensajeDto(
                    m.getId(),
                    m.getAutor().getNombreCompleto(),
                    m.getMensaje(),
                    m.isEsAdmin(),
                    m.getCreatedAt()
            );
        }
    }
}
