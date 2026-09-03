package com.integrador.Turismo.DTO;

import com.integrador.Turismo.Model.Reserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReservaResponse(
        String id,
        String paqueteId,
        String paqueteNombre,
        String fotoPrincipal,
        LocalDate fechaSalida,
        int numPersonas,
        BigDecimal precioTotal,
        String estado,
        LocalDateTime createdAt,
        List<AcompananteDto> acompanantes
) {
    public static ReservaResponse from(Reserva r) {
        List<AcompananteDto> acompanantes = r.getAcompanantes().stream()
                .map(a -> new AcompananteDto(
                        a.getNombreCompleto(),
                        a.getDniPasaporte(),
                        a.getPais(),
                        a.getFechaNacimiento(),
                        a.getGenero(),
                        a.getDatosAdicionales()
                ))
                .toList();

        String foto = r.getPaquete().getFotos().isEmpty()
                ? null
                : r.getPaquete().getFotos().get(0).getUrl();

        return new ReservaResponse(
                r.getId(),
                r.getPaquete().getId(),
                r.getPaquete().getNombre(),
                foto,
                r.getFechaSalida(),
                r.getNumPersonas(),
                r.getPrecioTotal(),
                r.getEstado().name(),
                r.getCreatedAt(),
                acompanantes
        );
    }
}
