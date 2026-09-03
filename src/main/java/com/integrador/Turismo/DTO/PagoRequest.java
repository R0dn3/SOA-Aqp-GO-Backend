package com.integrador.Turismo.DTO;

import com.integrador.Turismo.Model.Pago;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PagoRequest(
        @NotBlank String reservaId,
        @NotNull BigDecimal monto,
        @NotNull Pago.Metodo metodo,    // TRANSFERENCIA, TARJETA, YAPE, PLIN, EFECTIVO
        @NotBlank String referencia     // número de operación simulado
) {
}
