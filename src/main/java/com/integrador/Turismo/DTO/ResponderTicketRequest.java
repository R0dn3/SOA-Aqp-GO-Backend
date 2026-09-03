package com.integrador.Turismo.DTO;
import jakarta.validation.constraints.NotBlank;
public record ResponderTicketRequest(
        @NotBlank String mensaje
) {
}
