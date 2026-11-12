//Controller\AdminB2bController.java
package com.integrador.Turismo.Controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.integrador.Turismo.Repository.OrdenProveedorRepository;
import com.integrador.Turismo.Service.B2bProveedorService;

import lombok.RequiredArgsConstructor;

/** Panel B2B del administrador. /api/admin/** ya está restringido a ADMIN. */
@RestController
@RequestMapping("/api/admin/b2b")
@RequiredArgsConstructor
public class AdminB2bController {

    private final OrdenProveedorRepository ordenRepository;
    private final B2bProveedorService servicio;

    public record OrdenDto(String id, String reservaId, String proveedorId, String proveedorNombre,
            String tipoProveedor, String rol, String estado, String motivo, String detalle,
            String correlationId, LocalDateTime creadaEn, LocalDateTime actualizadaEn) {
    }

    public record ConfigB2bDto(boolean proveedoresDisponibles) {
    }

    public record DisponibilidadDto(boolean disponible) {
    }

    @GetMapping("/ordenes")
    public List<OrdenDto> ordenes() {
        return ordenRepository.findTop100ByOrderByCreadaEnDesc().stream()
                .map(o -> new OrdenDto(o.getId(), o.getReservaId(), o.getProveedorId(), o.getProveedorNombre(),
                        o.getTipoProveedor(), o.getRol(), o.getEstado(), o.getMotivo(), o.getDetalle(),
                        o.getCorrelationId(), o.getCreadaEn(), o.getActualizadaEn()))
                .toList();
    }

    @GetMapping("/config")
    public ConfigB2bDto config() {
        return new ConfigB2bDto(servicio.isDisponible());
    }

    @PatchMapping("/config")
    public ConfigB2bDto actualizar(@RequestBody ConfigB2bDto body) {
        servicio.setDisponible(body.proveedoresDisponibles());
        return new ConfigB2bDto(servicio.isDisponible());
    }

    // [POR PROVEEDOR] ids de los proveedores con caída simulada
    @GetMapping("/proveedores-caidos")
    public List<String> proveedoresCaidos() {
        return servicio.proveedoresCaidos();
    }

    @PatchMapping("/proveedores/{id}/disponibilidad")
    public List<String> disponibilidad(@PathVariable String id, @RequestBody DisponibilidadDto body) {
        servicio.setProveedorDisponible(id, body.disponible());
        return servicio.proveedoresCaidos();
    }
}