package com.integrador.Turismo.Controller;

import com.integrador.Turismo.DTO.PaqueteCreateDto;
import com.integrador.Turismo.DTO.PaqueteDetalleDto;
import com.integrador.Turismo.DTO.PaqueteResumenDto;
import com.integrador.Turismo.Service.PaqueteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/paquetes")
@RequiredArgsConstructor
public class PaqueteController {
    private final PaqueteService paqueteService;

    // ── Públicos ──────────────────────────────────────────────

    // GET /api/paquetes  →  lista de tarjetas
    @GetMapping
    public ResponseEntity<List<PaqueteResumenDto>> listar() {
        return ResponseEntity.ok(paqueteService.listarActivos());
    }

    // GET /api/paquetes/{id}  →  detalle completo
    @GetMapping("/{id}")
    public ResponseEntity<PaqueteDetalleDto> detalle(@PathVariable String id) {
        return ResponseEntity.ok(paqueteService.obtenerDetalle(id));
    }

    // ── Solo ADMIN ────────────────────────────────────────────

    // POST /api/paquetes  →  crear con fotos e itinerario en una sola llamada
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaqueteDetalleDto> crear(@Valid @RequestBody PaqueteCreateDto dto) {
        return ResponseEntity.ok(paqueteService.crear(dto));
    }

    // PUT /api/paquetes/{id}  →  editar reemplazando todo
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaqueteDetalleDto> actualizar(
            @PathVariable String id,
            @Valid @RequestBody PaqueteCreateDto dto) {
        return ResponseEntity.ok(paqueteService.actualizar(id, dto));
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaqueteDetalleDto> verpaqueteadmin(
            @PathVariable String id) {
        return ResponseEntity.ok(paqueteService.obtenerDetalle(id));
    }

    // PATCH /api/paquetes/{id}/toggle  →  activar/desactivar
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaqueteDetalleDto> toggleActivo(@PathVariable String id) {
        return ResponseEntity.ok(paqueteService.toggleActivo(id));
    }

    // DELETE /api/paquetes/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        paqueteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
