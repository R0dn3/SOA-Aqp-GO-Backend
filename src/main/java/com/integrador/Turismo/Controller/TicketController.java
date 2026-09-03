package com.integrador.Turismo.Controller;

import com.integrador.Turismo.DTO.*;
import com.integrador.Turismo.Model.Ticket;
import com.integrador.Turismo.Model.Usuario;
import com.integrador.Turismo.Service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor

public class TicketController {

    private final TicketService ticketService;

    // ──────────────────────────────────────────────────────────
    // CLIENTE
    // Base: /api/tickets
    // ──────────────────────────────────────────────────────────

    // POST /api/tickets
    // Crear ticket
    @PostMapping("/api/tickets")
    public ResponseEntity<?> crearTicket(
            @Valid @RequestBody CrearTicketRequest req,
            @AuthenticationPrincipal Usuario usuario
    ) {
        try {
            return ResponseEntity.ok(
                    ticketService.crear(req, usuario.getId())
            );
        } catch (Exception e) {
            return manejarError(e);
        }
    }

    // GET /api/tickets/mis-tickets
    // Ver mis tickets
    @GetMapping("/api/tickets/mis-tickets")
    public ResponseEntity<List<TicketResumenDto>> misTickets(
            @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(
                ticketService.misTickets(usuario.getId())
        );
    }

    // GET /api/tickets/{id}
    // Ver detalle de ticket propio
    @GetMapping("/api/tickets/{id}")
    public ResponseEntity<?> obtenerDetalle(
            @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario
    ) {
        try {
            return ResponseEntity.ok(
                    ticketService.obtenerDetalle(
                            id,
                            usuario.getId(),
                            false
                    )
            );
        } catch (Exception e) {
            return manejarError(e);
        }
    }

    // POST /api/tickets/{id}/responder
    // Cliente responde ticket
    @PostMapping("/api/tickets/{id}/responder")
    public ResponseEntity<?> responderCliente(
            @PathVariable String id,
            @Valid @RequestBody ResponderTicketRequest req,
            @AuthenticationPrincipal Usuario usuario
    ) {
        try {
            return ResponseEntity.ok(
                    ticketService.responderCliente(
                            id,
                            usuario.getId(),
                            req
                    )
            );
        } catch (Exception e) {
            return manejarError(e);
        }
    }

    // ──────────────────────────────────────────────────────────
    // ADMIN
    // Base: /api/admin/tickets
    // ──────────────────────────────────────────────────────────

    // GET /api/admin/tickets
    // Listar tickets paginados
    @GetMapping("/api/admin/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TicketResumenDto>> listarTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Ticket.Estado estado
    ) {

        return ResponseEntity.ok(
                ticketService.listarTodos(page, size, estado)
        );
    }

    // GET /api/admin/tickets/{id}
    // Admin ve cualquier ticket
    @GetMapping("/api/admin/tickets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> obtenerDetalleAdmin(
            @PathVariable String id,
            @AuthenticationPrincipal Usuario admin
    ) {
        try {
            return ResponseEntity.ok(
                    ticketService.obtenerDetalle(
                            id,
                            admin.getId(),
                            true
                    )
            );
        } catch (Exception e) {
            return manejarError(e);
        }
    }

    // POST /api/admin/tickets/{id}/responder
    // Admin responde
    @PostMapping("/api/admin/tickets/{id}/responder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> responderAdmin(
            @PathVariable String id,
            @Valid @RequestBody ResponderTicketRequest req,
            @AuthenticationPrincipal Usuario admin
    ) {
        try {
            return ResponseEntity.ok(
                    ticketService.responderAdmin(
                            id,
                            admin.getId(),
                            req
                    )
            );
        } catch (Exception e) {
            return manejarError(e);
        }
    }

    // PATCH /api/admin/tickets/{id}/estado
    @PatchMapping("/api/admin/tickets/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable String id,
            @Valid @RequestBody TicketEstadoRequest req
    ) {
        try {
            return ResponseEntity.ok(
                    ticketService.cambiarEstado(
                            id,
                            req.estado()
                    )
            );
        } catch (Exception e) {
            return manejarError(e);
        }
    }

    // PATCH /api/admin/tickets/{id}/prioridad
    @PatchMapping("/api/admin/tickets/{id}/prioridad")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cambiarPrioridad(
            @PathVariable String id,
            @Valid @RequestBody TicketPrioridadRequest req
    ) {
        try {
            return ResponseEntity.ok(
                    ticketService.cambiarPrioridad(
                            id,
                            req.prioridad()
                    )
            );
        } catch (Exception e) {
            return manejarError(e);
        }
    }

    // GET /api/admin/tickets/stats
    @GetMapping("/api/admin/tickets/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketStatsDto> stats() {
        return ResponseEntity.ok(
                ticketService.getStats()
        );
    }

    // ──────────────────────────────────────────────────────────
    // Helper de errores
    // ──────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, String>> manejarError(Exception e) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (e instanceof SecurityException) {
            status = HttpStatus.FORBIDDEN;
        } else if (e instanceof IllegalStateException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof RuntimeException) {
            status = HttpStatus.NOT_FOUND;
        }

        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "error", e.getClass().getSimpleName(),
                        "message", e.getMessage()
                ));
    }
}
