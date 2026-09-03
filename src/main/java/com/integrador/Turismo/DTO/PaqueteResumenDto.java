package com.integrador.Turismo.DTO;

import com.integrador.Turismo.Model.Paquete;

import java.math.BigDecimal;
import java.util.List;

// Vista de tarjeta — solo lo necesario para mostrar en la lista
public record PaqueteResumenDto (
        String id,
        String nombre,
        String subtitulo,
        BigDecimal precioBase,
        Integer duracionDias,
        Integer duracionNoches,
        String fotoPrincipal,       // primera foto del paquete
        List<String> lugares        // solo los nombres de los lugares
) {
    // Factory method — convierte entidad a DTO
    public static PaqueteResumenDto from(Paquete p) {
        String fotoPrincipal = p.getFotos().isEmpty()
                ? null
                : p.getFotos().get(0).getUrl();

        List<String> lugares = p.getLugares().stream()
                .map(l -> l.getNombre())
                .toList();

        return new PaqueteResumenDto(
                p.getId(),
                p.getNombre(),
                p.getSubtitulo(),
                p.getPrecioBase(),
                p.getDuracionDias(),
                p.getDuracionNoches(),
                fotoPrincipal,
                lugares
        );
    }
}

