package com.integrador.Turismo.DTO;

import com.integrador.Turismo.Model.Usuario;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank String nombreCompleto,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8) String password,
        String telefono,
        @NotNull Usuario.Rol rol
        ) {
}
