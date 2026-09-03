package com.integrador.Turismo.Service;

import com.integrador.Turismo.DTO.*;
import com.integrador.Turismo.Model.*;
import com.integrador.Turismo.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PaqueteRepository paqueteRepository;

    // ── Datos completos (un solo endpoint) ──────────────────
    public ReporteCompletoDto obtenerReportesCompletos() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1);
        LocalDate finMesAnterior = inicioMes.minusDays(1);

        // --- Resumen ---
        BigDecimal ingresosEsteMes = sumarIngresosConfirmados(inicioMes, finMes);
        BigDecimal ingresosMesAnterior = sumarIngresosConfirmados(inicioMesAnterior, finMesAnterior);
        double crecimiento = 0.0;
        if (ingresosMesAnterior.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = ingresosEsteMes.subtract(ingresosMesAnterior);
            crecimiento = diff.divide(ingresosMesAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        int totalReservas = (int) reservaRepository.count();
        int confirmadas = (int) reservaRepository.findByEstado(Reserva.Estado.CONFIRMADA).size();

        int nuevosClientesMes = (int) usuarioRepository.findAll().stream()
                .filter(u -> u.getCreatedAt().toLocalDate().isAfter(inicioMes.minusDays(1)))
                .count();

        ResumenReportesDto resumen = new ResumenReportesDto(
                ingresosEsteMes, totalReservas, confirmadas, crecimiento, nuevosClientesMes);

        // --- Ingresos mensuales (últimos 7 meses) ---
        List<IngresoMensualDto> ingresosMensuales = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate inicio = hoy.minusMonths(i).withDayOfMonth(1);
            LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());
            BigDecimal ingresos = sumarIngresosConfirmados(inicio, fin);
            int reservas = contarReservasEnMes(inicio, fin);
            String mes = inicio.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es"));
            ingresosMensuales.add(new IngresoMensualDto(mes, ingresos, reservas));
        }

        // --- Estados de reserva (todos) ---
        List<EstadoReservaDto> estados = Arrays.stream(Reserva.Estado.values())
                .map(e -> new EstadoReservaDto(e.name(), reservaRepository.findByEstado(e).size()))
                .collect(Collectors.toList());

        // --- Procedencia de clientes (por país) ---
        List<Usuario> usuarios = usuarioRepository.findAll();
        Map<String, Long> porPais = usuarios.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getPais() != null && !u.getPais().isBlank() ? u.getPais() : "No especificado",
                        Collectors.counting()));
        long totalUsuarios = usuarios.size();
        List<ProcedenciaClienteDto> procedencia = porPais.entrySet().stream()
                .map(e -> new ProcedenciaClienteDto(
                        e.getKey(),
                        e.getValue().intValue(),
                        totalUsuarios > 0 ? (e.getValue() * 100.0) / totalUsuarios : 0))
                .sorted((a, b) -> Integer.compare(b.clientes(), a.clientes()))
                .limit(6)  // top 6 países + otros
                .collect(Collectors.toList());
        if (procedencia.size() == 6) {
            int otrosClientes = (int) totalUsuarios - procedencia.stream().mapToInt(ProcedenciaClienteDto::clientes).sum();
            procedencia.add(new ProcedenciaClienteDto("Otros", otrosClientes, (otrosClientes * 100.0) / totalUsuarios));
        }

        // --- Reservas esta semana (lun a dom) ---
        LocalDate inicioSemana = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finSemana = hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<Reserva> reservasSemana = reservaRepository.findAll().stream()
                .filter(r -> !r.getCreatedAt().toLocalDate().isBefore(inicioSemana) &&
                        !r.getCreatedAt().toLocalDate().isAfter(finSemana))
                .toList();
        List<ReservaSemanaDto> semana = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            LocalDate dia = inicioSemana.plusDays(d);
            String nombreDia = dia.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es"));
            int nuevas = (int) reservasSemana.stream()
                    .filter(r -> r.getCreatedAt().toLocalDate().equals(dia))
                    .count();
            int canceladas = (int) reservasSemana.stream()
                    .filter(r -> r.getCreatedAt().toLocalDate().equals(dia) && r.getEstado() == Reserva.Estado.CANCELADA)
                    .count();
            semana.add(new ReservaSemanaDto(nombreDia, nuevas, canceladas));
        }

        // --- Rendimiento por paquete (top, sin cupos) ---
        List<Paquete> paquetes = paqueteRepository.findAll();
        List<RendimientoPaqueteDto> rendimiento = paquetes.stream()
                .map(p -> {
                    List<Reserva> reservasPaquete = reservaRepository.findByPaqueteId(p.getId());
                    int totalRes = reservasPaquete.size();
                    BigDecimal ingresos = reservasPaquete.stream()
                            .filter(r -> r.getEstado() == Reserva.Estado.CONFIRMADA || r.getEstado() == Reserva.Estado.COMPLETADA)
                            .map(Reserva::getPrecioTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new RendimientoPaqueteDto(p.getNombre(), totalRes, ingresos, p.getDuracionDias());
                })
                .sorted((a, b) -> b.ingresos().compareTo(a.ingresos()))
                .toList();

        return new ReporteCompletoDto(resumen, ingresosMensuales, estados, procedencia, semana, rendimiento);
    }

    // ── Métodos auxiliares ──────────────────────────────────
    private BigDecimal sumarIngresosConfirmados(LocalDate inicio, LocalDate fin) {
        return reservaRepository.findAll().stream()
                .filter(r -> (r.getEstado() == Reserva.Estado.CONFIRMADA || r.getEstado() == Reserva.Estado.COMPLETADA)
                        && !r.getCreatedAt().toLocalDate().isBefore(inicio)
                        && !r.getCreatedAt().toLocalDate().isAfter(fin))
                .map(Reserva::getPrecioTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int contarReservasEnMes(LocalDate inicio, LocalDate fin) {
        return (int) reservaRepository.findAll().stream()
                .filter(r -> !r.getCreatedAt().toLocalDate().isBefore(inicio)
                        && !r.getCreatedAt().toLocalDate().isAfter(fin))
                .count();
    }
}
