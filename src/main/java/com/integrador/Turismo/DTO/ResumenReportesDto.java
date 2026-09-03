package com.integrador.Turismo.DTO;

import java.math.BigDecimal;

public record ResumenReportesDto(
        BigDecimal ingresosEsteMes,
        int totalReservas,
        int confirmadas,
        double crecimientoIngresos,   // % respecto al mes anterior
        int nuevosClientesMes
) {
}
