package com.integrador.Turismo.DTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

// DTO unificado para crear Y editar un paquete
// El frontend manda todo en una sola petición
public record PaqueteCreateDto(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String subtitulo,

        // Textos en formato Markdown
        String resumenMd,
        String recomendacionesMd,
        String incluyeMd,
        String noIncluyeMd,
        String preguntasMd,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        BigDecimal precioBase,

        @NotNull @Min(value = 1, message = "La duración mínima es 1 día")
        Integer duracionDias,

        Integer duracionNoches,

        String mapaUrl,

        // IDs de lugares ya existentes en BD
        List<String> lugarIds,

        // Proveedores asignados con su rol en este paquete
        @Valid
        List<ProveedorAsignadoDto> proveedores,

        // Fotos ya subidas a Cloudinary — se mandan las URLs
        @Valid
        List<FotoDto> fotos,

        // Itinerario completo
        @Valid
        List<ItinerarioDiaDto> itinerario
) {
    // ── Nested DTOs ───────────────────────────────────────────

    public record FotoDto(
            @NotBlank(message = "La URL de la foto es obligatoria")
            String url,
            String alt,
            Integer orden
    ) {}

    public record ItinerarioDiaDto(
            @NotNull @Min(1)
            Integer diaNumero,

            @NotBlank(message = "El título del día es obligatorio")
            String titulo,

            String descripcionMd
    ) {}
    public record ProveedorAsignadoDto(
            @NotBlank(message = "El ID del proveedor es obligatorio")
            String proveedorId,

            // Rol en este paquete específico: TRANSPORTE, HOTEL, RESTAURANTE
            String rol,
            String notas
    ) {}
}
