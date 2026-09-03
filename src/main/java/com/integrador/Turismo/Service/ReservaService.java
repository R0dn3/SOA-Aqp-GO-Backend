package com.integrador.Turismo.Service;

import com.integrador.Turismo.DTO.AcompananteDto;
import com.integrador.Turismo.DTO.ReservaRequest;
import com.integrador.Turismo.DTO.ReservaResponse;
import com.integrador.Turismo.Model.*;
import com.integrador.Turismo.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaService {
    private final ReservaRepository reservaRepository;
    private final PaqueteRepository paqueteRepository;
    private final UsuarioRepository usuarioRepository;

    // Cliente — crear reserva
    @Transactional
    public ReservaResponse crear(ReservaRequest req, String usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Paquete paquete = paqueteRepository.findById(req.paqueteId())
                .orElseThrow(() -> new RuntimeException("Paquete no encontrado"));

        if (!paquete.getActivo()) {
            throw new IllegalStateException("El paquete no está disponible");
        }

        Reserva reserva = Reserva.builder()
                .usuario(usuario)
                .paquete(paquete)
                .fechaSalida(req.fechaSalida())
                .numPersonas(req.numPersonas())
                .precioTotal(paquete.getPrecioBase()
                        .multiply(java.math.BigDecimal.valueOf(req.numPersonas())))
                .estado(Reserva.Estado.PENDIENTE_PAGO)
                .build();

        if (req.acompanantes() != null) {
            for (AcompananteDto dto : req.acompanantes()) {
                Acompanante a = Acompanante.builder()
                        .reserva(reserva)
                        .nombreCompleto(dto.nombreCompleto())
                        .dniPasaporte(dto.dniPasaporte())
                        .pais(dto.pais())
                        .fechaNacimiento(dto.fechaNacimiento())
                        .genero(dto.genero())
                        .datosAdicionales(dto.datosAdicionales())
                        .build();
                reserva.getAcompanantes().add(a);
            }
        }

        return ReservaResponse.from(reservaRepository.save(reserva));
    }

    // Cliente — mis reservas
    @Transactional(readOnly = true)
    public List<ReservaResponse> misReservas(String usuarioId) {
        return reservaRepository.findByUsuarioId(usuarioId)
                .stream().map(ReservaResponse::from).toList();
    }

    // Cliente/Admin — detalle de una reserva
    @Transactional(readOnly = true)
    public ReservaResponse obtenerPorId(String id) {
        return ReservaResponse.from(
                reservaRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Reserva no encontrada")));
    }

    // Admin — todas las reservas
    public List<ReservaResponse> listarTodas() {
        return reservaRepository.findAll()
                .stream().map(ReservaResponse::from).toList();
    }

    // Admin — cambiar estado manualmente
    @Transactional
    public ReservaResponse cambiarEstado(String id, Reserva.Estado nuevoEstado) {
        log.info("Cambiando estado de reserva {} a {}", id, nuevoEstado);
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        log.info("Reserva encontrada: {}", reserva);
        reserva.setEstado(nuevoEstado);
        log.info("Guardado exitoso");
        return ReservaResponse.from(reservaRepository.save(reserva));
    }

    // Cliente — cancelar su propia reserva
    @Transactional
    public ReservaResponse cancelar(String id, String usuarioId) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para cancelar esta reserva");
        }
        if (reserva.getEstado() == Reserva.Estado.COMPLETADA) {
            throw new IllegalStateException("No se puede cancelar una reserva completada");
        }
        reserva.setEstado(Reserva.Estado.CANCELADA);
        return ReservaResponse.from(reservaRepository.save(reserva));
    }
}
