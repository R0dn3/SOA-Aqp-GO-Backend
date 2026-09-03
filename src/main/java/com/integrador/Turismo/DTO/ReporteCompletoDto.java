package com.integrador.Turismo.DTO;

import java.util.List;

public record ReporteCompletoDto(
        ResumenReportesDto resumen,
        List<IngresoMensualDto> ingresosMensuales,
        List<EstadoReservaDto> estadosReserva,
        List<ProcedenciaClienteDto> procedenciaClientes,
        List<ReservaSemanaDto> reservasSemana,
        List<RendimientoPaqueteDto> rendimientoPaquetes
) {
}
