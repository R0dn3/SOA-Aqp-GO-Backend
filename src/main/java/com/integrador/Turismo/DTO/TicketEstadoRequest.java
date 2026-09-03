package com.integrador.Turismo.DTO;

import com.integrador.Turismo.Model.Ticket;
import jakarta.validation.constraints.NotNull;

public record TicketEstadoRequest(
        @NotNull
        Ticket.Estado estado
) {
}
