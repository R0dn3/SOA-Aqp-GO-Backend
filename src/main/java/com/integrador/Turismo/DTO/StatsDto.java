package com.integrador.Turismo.DTO;

import java.math.BigDecimal;

public record StatsDto(
        long totalPaquetes,
        long totalReservas,
        long totalUsuarios,
        long totalProveedores,
        long reservasHoy,
        long reservasPendientesPago,
        BigDecimal ingresosTotales
) {
}
