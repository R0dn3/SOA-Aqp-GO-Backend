package com.integrador.Turismo.DTO;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public record ReservaRequest(
        @NotBlank String paqueteId,
        @NotNull @Future LocalDate fechaSalida,
        @Min(1) int numPersonas,
        List<AcompananteDto> acompanantes
) {
}
