package com.integrador.Turismo.DTO;
import com.integrador.Turismo.Model.Ticket;
import jakarta.validation.constraints.NotNull;

public record TicketPrioridadRequest(
        @NotNull
        Ticket.Prioridad prioridad
) {
}
