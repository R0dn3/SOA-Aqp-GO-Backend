package com.integrador.Turismo.DTO;
import com.integrador.Turismo.Model.Itinerariodia;
import com.integrador.Turismo.Model.Paquete;
import com.integrador.Turismo.Model.Paquetefoto;

import java.math.BigDecimal;
import java.util.List;

// Vista de detalle — todos los campos del paquete
public record PaqueteDetalleDto(
        String id,
        String nombre,
        String subtitulo,
        String resumenMd,
        String recomendacionesMd,
        String incluyeMd,
        String noIncluyeMd,
        String preguntasMd,
        BigDecimal precioBase,
        Integer duracionDias,
        Integer duracionNoches,
        String mapaUrl,
        List<FotoDto> fotos,
        List<String> lugares,
        List<ItinerarioDto> itinerario
) {
    // Factory method
    public static PaqueteDetalleDto from(Paquete p) {
        List<FotoDto> fotos = p.getFotos().stream()
                .map(f -> new FotoDto(f.getId(), f.getUrl(), f.getAlt(), f.getOrden()))
                .toList();

        List<String> lugares = p.getLugares().stream()
                .map(l -> l.getNombre())
                .toList();

        List<ItinerarioDto> itinerario = p.getItinerario().stream()
                .map(d -> new ItinerarioDto(
                        d.getDiaNumero(),
                        d.getTitulo(),
                        d.getDescripcionMd()))
                .toList();

        return new PaqueteDetalleDto(
                p.getId(),
                p.getNombre(),
                p.getSubtitulo(),
                p.getResumenMd(),
                p.getRecomendacionesMd(),
                p.getIncluyeMd(),
                p.getNoIncluyeMd(),
                p.getPreguntasMd(),
                p.getPrecioBase(),
                p.getDuracionDias(),
                p.getDuracionNoches(),
                p.getMapaUrl(),
                fotos,
                lugares,
                itinerario
        );
    }

    // DTOs internos (nested records)
    public record FotoDto(
            String id,
            String url,
            String alt,
            Integer orden
    ) {}

    public record ItinerarioDto(
            Integer diaNumero,
            String titulo,
            String descripcionMd
    ) {}
}
