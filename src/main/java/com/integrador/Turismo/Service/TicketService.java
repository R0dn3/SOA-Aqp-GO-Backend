package com.integrador.Turismo.Service;

import com.integrador.Turismo.DTO.*;
import com.integrador.Turismo.Model.*;
import com.integrador.Turismo.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository        ticketRepository;
    private final TicketMensajeRepository mensajeRepository;
    private final UsuarioRepository       usuarioRepository;
    private final ReservaRepository       reservaRepository;

    // ── Cliente: crear ticket ─────────────────────────────────

    @Transactional
    public TicketDetalleDto crear(CrearTicketRequest req, String usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Busca la reserva si se proporcionó
        Reserva reserva = null;
        if (req.reservaId() != null && !req.reservaId().isBlank()) {
            reserva = reservaRepository.findById(req.reservaId())
                    .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
            // Verifica que la reserva pertenece al usuario
            if (!reserva.getUsuario().getId().equals(usuarioId)) {
                throw new SecurityException("La reserva no te pertenece");
            }
        }

        Ticket ticket = Ticket.builder()
                .usuario(usuario)
                .reserva(reserva)
                .tipo(req.tipo())
                .asunto(req.asunto())
                .estado(Ticket.Estado.ABIERTO)
                .prioridad(Ticket.Prioridad.MEDIA)
                .build();

        // El mensaje inicial forma parte del ticket
        TicketMensaje mensajeInicial = TicketMensaje.builder()
                .ticket(ticket)
                .autor(usuario)
                .mensaje(req.mensajeInicial())
                .esAdmin(false)
                .build();

        ticket.getMensajes().add(mensajeInicial);
        ticketRepository.save(ticket);

        return TicketDetalleDto.from(ticket);
    }

    // ── Cliente: mis tickets ──────────────────────────────────

    public List<TicketResumenDto> misTickets(String usuarioId) {
        return ticketRepository
                .findByUsuarioIdOrderByUpdatedAtDesc(usuarioId)
                .stream()
                .map(TicketResumenDto::from)
                .toList();
    }

    // ── Cliente: responder en su propio ticket ────────────────

    @Transactional
    public TicketDetalleDto responderCliente(String ticketId,
                                             String usuarioId,
                                             ResponderTicketRequest req) {
        Ticket ticket = obtenerConMensajes(ticketId);

        // Verifica que el ticket pertenece al usuario
        if (!ticket.getUsuario().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes acceso a este ticket");
        }
        if (ticket.getEstado() == Ticket.Estado.CERRADO) {
            throw new IllegalStateException("No puedes responder a un ticket cerrado");
        }

        Usuario autor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        agregarMensaje(ticket, autor, req.mensaje(), false);

        // Si estaba resuelto y el cliente responde, vuelve a EN_PROCESO
        if (ticket.getEstado() == Ticket.Estado.RESUELTO) {
            ticket.setEstado(Ticket.Estado.EN_PROCESO);
        }

        return TicketDetalleDto.from(ticketRepository.save(ticket));
    }

    // ── Detalle (cliente y admin) ─────────────────────────────

    public TicketDetalleDto obtenerDetalle(String ticketId, String usuarioId, boolean esAdmin) {
        Ticket ticket = obtenerConMensajes(ticketId);

        // El cliente solo puede ver sus propios tickets
        if (!esAdmin && !ticket.getUsuario().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes acceso a este ticket");
        }

        return TicketDetalleDto.from(ticket);
    }

    // ── Admin: todos los tickets paginados ────────────────────

    public Page<TicketResumenDto> listarTodos(int page, int size, Ticket.Estado estado) {
        Pageable pageable = PageRequest.of(page, size);

        if (estado != null) {
            return ticketRepository
                    .findByEstadoOrderByUpdatedAtDesc(estado, pageable)
                    .map(TicketResumenDto::from);
        }
        return ticketRepository
                .findAllByOrderByUpdatedAtDesc(pageable)
                .map(TicketResumenDto::from);
    }

    // ── Admin: responder ticket ───────────────────────────────

    @Transactional
    public TicketDetalleDto responderAdmin(String ticketId,
                                           String adminId,
                                           ResponderTicketRequest req) {
        Ticket ticket = obtenerConMensajes(ticketId);

        if (ticket.getEstado() == Ticket.Estado.CERRADO) {
            throw new IllegalStateException("No puedes responder a un ticket cerrado");
        }

        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        // Asigna el admin si no estaba asignado
        if (ticket.getAdminAsignado() == null) {
            ticket.setAdminAsignado(admin);
        }

        // Cambia estado a EN_PROCESO automáticamente
        if (ticket.getEstado() == Ticket.Estado.ABIERTO) {
            ticket.setEstado(Ticket.Estado.EN_PROCESO);
        }

        agregarMensaje(ticket, admin, req.mensaje(), true);

        return TicketDetalleDto.from(ticketRepository.save(ticket));
    }

    // ── Admin: cambiar estado ─────────────────────────────────

    @Transactional
    public TicketDetalleDto cambiarEstado(String ticketId, Ticket.Estado nuevoEstado) {
        Ticket ticket = obtenerConMensajes(ticketId);
        ticket.setEstado(nuevoEstado);
        return TicketDetalleDto.from(ticketRepository.save(ticket));
    }

    // ── Admin: cambiar prioridad ──────────────────────────────

    @Transactional
    public TicketDetalleDto cambiarPrioridad(String ticketId, Ticket.Prioridad prioridad) {
        Ticket ticket = obtenerConMensajes(ticketId);
        ticket.setPrioridad(prioridad);
        return TicketDetalleDto.from(ticketRepository.save(ticket));
    }

    // ── Admin: stats para dashboard ───────────────────────────

    public TicketStatsDto getStats() {
        long total     = ticketRepository.count();
        long abiertos  = ticketRepository.countByEstado(Ticket.Estado.ABIERTO);
        long enProceso = ticketRepository.countByEstado(Ticket.Estado.EN_PROCESO);
        long resueltos = ticketRepository.countByEstado(Ticket.Estado.RESUELTO);
        long cerrados  = ticketRepository.countByEstado(Ticket.Estado.CERRADO);
        long sinAsignar = ticketRepository
                .countByAdminAsignadoIsNullAndEstadoNot(Ticket.Estado.CERRADO);

        return new TicketStatsDto(total, abiertos, enProceso,
                resueltos, cerrados, sinAsignar);
    }

    // ── Helpers privados ──────────────────────────────────────

    private Ticket obtenerConMensajes(String ticketId) {
        return ticketRepository.findByIdWithMensajes(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado: " + ticketId));
    }

    private void agregarMensaje(Ticket ticket, Usuario autor,
                                String texto, boolean esAdmin) {
        TicketMensaje mensaje = TicketMensaje.builder()
                .ticket(ticket)
                .autor(autor)
                .mensaje(texto)
                .esAdmin(esAdmin)
                .build();
        ticket.getMensajes().add(mensaje);
    }
}
