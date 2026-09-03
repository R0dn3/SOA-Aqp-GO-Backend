package com.integrador.Turismo.Controller;

import com.integrador.Turismo.DTO.ReporteCompletoDto;
import com.integrador.Turismo.Service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reportes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReporteController {
    private final ReporteService reporteService;

    @GetMapping
    public ResponseEntity<ReporteCompletoDto> obtenerReportes() {
        return ResponseEntity.ok(reporteService.obtenerReportesCompletos());
    }
}
