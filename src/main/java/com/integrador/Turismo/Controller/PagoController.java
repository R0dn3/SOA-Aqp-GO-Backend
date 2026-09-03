package com.integrador.Turismo.Controller;

import com.integrador.Turismo.DTO.PagoRequest;
import com.integrador.Turismo.DTO.PagoResponse;
import com.integrador.Turismo.Service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    // POST /api/pagos
    // Procesa el pago y confirma la reserva si es exitoso
    @PostMapping
    public ResponseEntity<PagoResponse> procesarPago(
            @Valid @RequestBody PagoRequest req) {
        return ResponseEntity.ok(pagoService.procesarPago(req));
    }
}
