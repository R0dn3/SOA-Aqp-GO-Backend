package com.integrador.Turismo.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponse(
        String id,
        String reservaId,
        BigDecimal monto,
        String metodo,
        String estado,          // PENDIENTE, VERIFICADO, RECHAZADO
        String referencia,
        LocalDateTime fechaPago
) {
}
