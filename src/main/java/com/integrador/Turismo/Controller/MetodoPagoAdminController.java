package com.integrador.Turismo.Controller;

import com.integrador.Turismo.DTO.ActivoRequest;
import com.integrador.Turismo.DTO.MetodoPagoConfigDto;
import com.integrador.Turismo.Model.Pago;
import com.integrador.Turismo.Service.MetodoPagoConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/metodos-pago")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MetodoPagoAdminController {

    private final MetodoPagoConfigService service;

    @GetMapping
    public List<MetodoPagoConfigDto> listar() {
        return service.listarTodos();
    }

    @PatchMapping("/{metodo}")
    public MetodoPagoConfigDto actualizar(@PathVariable Pago.Metodo metodo, @RequestBody ActivoRequest req) {
        return service.actualizar(metodo, req.activo());
    }
}