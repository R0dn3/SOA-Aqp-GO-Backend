package com.integrador.Turismo.DTO;

import java.time.LocalDate;

public record ActualizarPerfilRequest(
        String nombreCompleto,
        String telefono,
        String dniPasaporte,
        String pais,
        LocalDate fechaNacimiento,
        String genero
) {
}
