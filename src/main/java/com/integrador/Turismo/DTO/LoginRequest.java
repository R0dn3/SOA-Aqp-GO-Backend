package com.integrador.Turismo.DTO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
) {
}
