package com.integrador.Turismo.Controller;

import com.integrador.Turismo.DTO.ReservaEstadoRequest;
import com.integrador.Turismo.DTO.ReservaRequest;
import com.integrador.Turismo.DTO.ReservaResponse;
import com.integrador.Turismo.Model.Reserva;
import com.integrador.Turismo.Model.Usuario;
import com.integrador.Turismo.Service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {
    private final ReservaService reservaService;

    // ── Cliente autenticado ───────────────────────────────────

    // POST /api/reservas
    // El usuario logueado crea su propia reserva
    @PostMapping
    public ResponseEntity<ReservaResponse> crear(
            @Valid @RequestBody ReservaRequest req,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reservaService.crear(req, usuario.getId()));
    }

    // GET /api/reservas/mis-reservas
    // El cliente ve solo sus reservas
    @GetMapping("/mis-reservas")
    public ResponseEntity<List<ReservaResponse>> misReservas(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reservaService.misReservas(usuario.getId()));
    }

    // GET /api/reservas/{id}
    // Cliente ve el detalle de una de sus reservas
    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> detalle(@PathVariable String id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    // PATCH /api/reservas/{id}/cancelar
    // El cliente cancela su propia reserva
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponse> cancelar(
            @PathVariable String id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reservaService.cancelar(id, usuario.getId()));
    }

    // ── Solo ADMIN ────────────────────────────────────────────

    // GET /api/reservas
    // Admin ve todas las reservas
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponse>> listarTodas() {
        return ResponseEntity.ok(reservaService.listarTodas());
    }

    // PATCH /api/reservas/{id}/estado
    // Admin cambia el estado manualmente
    // Body: "CONFIRMADA" | "CANCELADA" | "COMPLETADA" | "PENDIENTE_PAGO"
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cambiarEstado(@PathVariable String id, @RequestBody ReservaEstadoRequest req) {
        try {
            return ResponseEntity.ok(reservaService.cambiarEstado(id, req.estado()));
        } catch (Exception e) {
            e.printStackTrace(); // Esto imprimirá el stack trace en la consola del backend
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getClass().getName(), "message", e.getMessage()));
        }
    }
}
