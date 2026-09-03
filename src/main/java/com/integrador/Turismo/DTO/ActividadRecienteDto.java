package com.integrador.Turismo.DTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ActividadRecienteDto(
        String id,
        String clienteNombre,
        String clienteEmail,
        String paqueteNombre,
        LocalDate fechaSalida,
        int numPersonas,
        BigDecimal precioTotal,
        String estado,
        LocalDateTime createdAt
) {
}
