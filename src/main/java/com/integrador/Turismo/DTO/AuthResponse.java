package com.integrador.Turismo.DTO;

public record AuthResponse(
        String token,
        String email,
        String nombreCompleto,
        String rol
) {
}
