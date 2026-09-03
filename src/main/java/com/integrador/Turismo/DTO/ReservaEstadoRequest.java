package com.integrador.Turismo.DTO;

import com.integrador.Turismo.Model.Reserva;
import jakarta.validation.constraints.NotNull;

public record ReservaEstadoRequest(
        @NotNull Reserva.Estado estado
) {
}
