package com.integrador.Turismo.Controller;

import com.integrador.Turismo.DTO.*;
import com.integrador.Turismo.Service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;


    // GET /api/admin/stats
    @GetMapping("/stats")
    public ResponseEntity<StatsDto> stats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    // GET /api/admin/actividad-reciente
    @GetMapping("/actividad-reciente")
    public ResponseEntity<List<ActividadRecienteDto>> actividadReciente() {
        return ResponseEntity.ok(adminService.getActividadReciente());
    }

    // GET /api/admin/paquetes
    @GetMapping("/paquetes")
    public ResponseEntity<List<PaqueteAdminDto>> paquetes() {
        return ResponseEntity.ok(adminService.getPaquetesAdmin());
    }

    // GET /api/admin/pagos
    @GetMapping("/pagos")
    public ResponseEntity<List<PagoAdminDto>> pagos() {
        return ResponseEntity.ok(adminService.getPagosAdmin());
    }

    // GET /api/admin/pagos/stats
    @GetMapping("/pagos/stats")
    public ResponseEntity<PagoStatsDto> pagoStats() {
        return ResponseEntity.ok(adminService.getPagoStats());
    }
}
