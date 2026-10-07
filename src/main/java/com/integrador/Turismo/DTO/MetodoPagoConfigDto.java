package com.integrador.Turismo.DTO;

import com.integrador.Turismo.Model.Pago;

public record MetodoPagoConfigDto(
        Pago.Metodo metodo,
        boolean activo) {
}