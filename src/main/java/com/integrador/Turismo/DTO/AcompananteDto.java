package com.integrador.Turismo.DTO;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
public record AcompananteDto(
        @NotBlank String nombreCompleto,
        @NotBlank String dniPasaporte,
        String pais,
        LocalDate fechaNacimiento,
        String genero,
        String datosAdicionales
) {
}
