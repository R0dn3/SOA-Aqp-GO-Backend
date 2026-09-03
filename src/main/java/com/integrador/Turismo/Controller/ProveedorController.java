package com.integrador.Turismo.Controller;

import com.integrador.Turismo.Model.Proveedor;
import com.integrador.Turismo.Service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {
    private final ProveedorService proveedorService;

    // GET /api/proveedores — todos (admin)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Proveedor>> listar() {
        return ResponseEntity.ok(proveedorService.listar());
    }

    // GET /api/proveedores?tipo=HOTEL — filtrar por tipo
    @GetMapping("/tipo/{tipo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Proveedor>> listarPorTipo(
            @PathVariable Proveedor.Tipo tipo) {
        return ResponseEntity.ok(proveedorService.listarPorTipo(tipo));
    }

    // GET /api/proveedores/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Proveedor> obtener(@PathVariable String id) {
        return ResponseEntity.ok(proveedorService.obtenerPorId(id));
    }

    // POST /api/proveedores
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Proveedor> crear(@Valid @RequestBody Proveedor proveedor) {
        return ResponseEntity.ok(proveedorService.crear(proveedor));
    }

    // PUT /api/proveedores/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Proveedor> actualizar(@PathVariable String id,
                                                @Valid @RequestBody Proveedor datos) {
        return ResponseEntity.ok(proveedorService.actualizar(id, datos));
    }

    // DELETE /api/proveedores/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        proveedorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
