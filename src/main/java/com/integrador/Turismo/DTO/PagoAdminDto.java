package com.integrador.Turismo.DTO;

import com.integrador.Turismo.Model.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoAdminDto(
        String id,
        String reservaId,
        String clienteNombre,
        String clienteEmail,
        String paqueteNombre,
        BigDecimal monto,
        String metodo,
        String estado,
        String referencia,
        LocalDateTime fechaPago
) {
    public static PagoAdminDto from(Pago p) {
        return new PagoAdminDto(
                p.getId(),
                p.getReserva().getId(),
                p.getReserva().getUsuario().getNombreCompleto(),
                p.getReserva().getUsuario().getEmail(),
                p.getReserva().getPaquete().getNombre(),
                p.getMonto(),
                p.getMetodo().name(),
                p.getEstado().name(),
                p.getReferencia(),
                p.getFechaPago()
        );
    }
}
