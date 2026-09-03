package com.integrador.Turismo.DTO;

public record TicketStatsDto(
        long totalTickets,
        long abiertos,
        long enProceso,
        long resueltos,
        long cerrados,
        long sinAsignar
) {
}
