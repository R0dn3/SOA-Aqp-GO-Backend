package com.integrador.Turismo.DTO;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
        @NotBlank String idToken  // token que manda Firebase desde el frontend
) {
}
