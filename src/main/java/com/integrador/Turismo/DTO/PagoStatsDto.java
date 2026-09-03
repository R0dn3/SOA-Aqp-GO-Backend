package com.integrador.Turismo.DTO;

import java.math.BigDecimal;

public record PagoStatsDto(
        long totalPagos,
        long pagosVerificados,
        long pagosPendientes,
        long pagosRechazados,
        BigDecimal montoTotalVerificado
) {
}
