package com.integrador.Turismo.DTO;

import java.math.BigDecimal;

public record RendimientoPaqueteDto(
        String nombre,
        int reservas,
        BigDecimal ingresos,
        int duracionDias
) {
}
