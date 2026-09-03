package com.integrador.Turismo.Service;

import com.integrador.Turismo.DTO.*;
import com.integrador.Turismo.Model.Pago;
import com.integrador.Turismo.Model.Reserva;
import com.integrador.Turismo.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final PaqueteRepository   paqueteRepository;
    private final ReservaRepository   reservaRepository;
    private final UsuarioRepository   usuarioRepository;
    private final ProveedorRepository proveedorRepository;
    private final PagoRepository pagoRepository;

    // ── Stats generales del dashboard ────────────────────────
    public StatsDto getStats() {
        long totalPaquetes    = paqueteRepository.count();
        long totalReservas    = reservaRepository.count();
        long totalUsuarios    = usuarioRepository.count();
        long totalProveedores = proveedorRepository.count();

        long reservasHoy = reservaRepository
                .findAll()
                .stream()
                .filter(r -> r.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                .count();

        long pendientesPago = reservaRepository
                .findByEstado(Reserva.Estado.PENDIENTE_PAGO)
                .size();

        BigDecimal ingresos = reservaRepository
                .findByEstado(Reserva.Estado.CONFIRMADA)
                .stream()
                .map(Reserva::getPrecioTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StatsDto(
                totalPaquetes, totalReservas, totalUsuarios, totalProveedores,
                reservasHoy, pendientesPago, ingresos
        );
    }

    // ── Últimas 10 reservas para actividad reciente ───────────
    public List<ActividadRecienteDto> getActividadReciente() {
        return reservaRepository
                .findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(r -> new ActividadRecienteDto(
                        r.getId(),
                        r.getUsuario().getNombreCompleto(),
                        r.getUsuario().getEmail(),
                        r.getPaquete().getNombre(),
                        r.getFechaSalida(),
                        r.getNumPersonas(),
                        r.getPrecioTotal(),
                        r.getEstado().name(),
                        r.getCreatedAt()
                ))
                .toList();
    }

    // ── Lista de paquetes para la tabla del admin ─────────────
    public List<PaqueteAdminDto> getPaquetesAdmin() {
        return paqueteRepository.findAll().stream().map(paquete -> {
            List<Reserva> reservas = reservaRepository.findByPaqueteId(paquete.getId());

            long totalReservas = reservas.size();

            BigDecimal ingresos = reservas.stream()
                    .filter(r -> r.getEstado() == Reserva.Estado.CONFIRMADA
                            || r.getEstado() == Reserva.Estado.COMPLETADA)
                    .map(Reserva::getPrecioTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return PaqueteAdminDto.from(paquete, totalReservas, ingresos);
        }).toList();
    }

    public List<PagoAdminDto> getPagosAdmin() {
        return pagoRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaPago"))
                .stream()
                .map(PagoAdminDto::from)
                .toList();
    }

    public PagoStatsDto getPagoStats() {
        List<Pago> todos = pagoRepository.findAll();

        long verificados  = todos.stream().filter(p -> p.getEstado() == Pago.Estado.VERIFICADO).count();
        long pendientes   = todos.stream().filter(p -> p.getEstado() == Pago.Estado.PENDIENTE).count();
        long rechazados   = todos.stream().filter(p -> p.getEstado() == Pago.Estado.RECHAZADO).count();

        BigDecimal montoTotal = todos.stream()
                .filter(p -> p.getEstado() == Pago.Estado.VERIFICADO)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PagoStatsDto(todos.size(), verificados, pendientes, rechazados, montoTotal);
    }
}
