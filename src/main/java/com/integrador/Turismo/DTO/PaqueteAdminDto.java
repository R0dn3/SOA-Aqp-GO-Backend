package com.integrador.Turismo.DTO;

import com.integrador.Turismo.Model.Paquete;

import java.math.BigDecimal;
import java.util.List;

public record PaqueteAdminDto(
        String id,
        String nombre,
        List<String> lugares,
        BigDecimal precioBase,
        int duracionDias,
        int duracionNoches,
        long totalReservas,
        BigDecimal ingresosTotales,
        String fotoPrincipal,
        boolean activo,
        String createdAt
) {
    public static PaqueteAdminDto from(Paquete p, long totalReservas, BigDecimal ingresosTotales) {
        return new PaqueteAdminDto(
                p.getId(),
                p.getNombre(),
                p.getLugares().stream().map(l -> l.getNombre()).toList(),
                p.getPrecioBase(),
                p.getDuracionDias(),
                p.getDuracionNoches() != null ? p.getDuracionNoches() : 0,
                totalReservas,
                ingresosTotales,
                p.getFotos().isEmpty() ? null : p.getFotos().get(0).getUrl(),
                p.getActivo(),
                p.getCreatedAt().toLocalDate().toString()
        );
    }
}
