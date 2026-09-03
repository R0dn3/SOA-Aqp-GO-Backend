package com.integrador.Turismo.DTO;

import java.math.BigDecimal;

public record IngresoMensualDto(
        String mes,
        BigDecimal ingresos,
        int reservas
) {
}
