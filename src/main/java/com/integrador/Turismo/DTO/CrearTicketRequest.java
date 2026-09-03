package com.integrador.Turismo.DTO;

import com.integrador.Turismo.Model.Ticket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearTicketRequest(
        @NotNull  Ticket.Tipo    tipo,
        @NotBlank String         asunto,
        @NotBlank String         mensajeInicial,  // primer mensaje del hilo
        String                   reservaId        // opcional
) {
}
