package com.integrador.Turismo.Controller;
import com.integrador.Turismo.Model.Lugar;
import com.integrador.Turismo.Service.LugarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lugares")
@RequiredArgsConstructor
public class LugarController {
    private final LugarService lugarService;

    // GET /api/lugares — público
    @GetMapping
    public ResponseEntity<List<Lugar>> listar() {
        return ResponseEntity.ok(lugarService.listar());
    }

    // GET /api/lugares/{id} — público
    @GetMapping("/{id}")
    public ResponseEntity<Lugar> obtener(@PathVariable String id) {
        return ResponseEntity.ok(lugarService.obtenerPorId(id));
    }

    // POST /api/lugares — solo ADMIN
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Lugar> crear(@Valid @RequestBody Lugar lugar) {
        return ResponseEntity.ok(lugarService.crear(lugar));
    }

    // PUT /api/lugares/{id} — solo ADMIN
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Lugar> actualizar(@PathVariable String id,
                                            @Valid @RequestBody Lugar datos) {
        return ResponseEntity.ok(lugarService.actualizar(id, datos));
    }

    // DELETE /api/lugares/{id} — solo ADMIN
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        lugarService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
