//Controller\B2bProveedorController.java
package com.integrador.Turismo.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.integrador.Turismo.Service.B2bProveedorService;
import com.integrador.Turismo.Service.B2bProveedorService.RespuestaB2b;
import com.integrador.Turismo.Service.B2bProveedorService.Resultado;
import com.integrador.Turismo.Service.B2bProveedorService.SolicitudB2b;

import lombok.RequiredArgsConstructor;

/**
 * API B2B del proveedor. No usa JWT: se autentica con el header X-B2B-Token
 * (SecurityConfig la deja pasar y el servicio valida el token).
 * Códigos: 200 respuesta de negocio, 401 token, 409 duplicado, 422 documento
 * inválido, 503 proveedor no disponible.
 */
@RestController
@RequestMapping("/api/b2b/proveedores")
@RequiredArgsConstructor
public class B2bProveedorController {

    private final B2bProveedorService servicio;

    @PostMapping("/ordenes")
    public ResponseEntity<RespuestaB2b> orden(
            @RequestHeader(value = "X-B2B-Token", required = false) String token,
            @RequestBody SolicitudB2b body) {
        Resultado r = servicio.recibirOrden(token, body);
        return ResponseEntity.status(r.status()).body(r.cuerpo());
    }

    @PostMapping("/cambios")
    public ResponseEntity<RespuestaB2b> cambio(
            @RequestHeader(value = "X-B2B-Token", required = false) String token,
            @RequestBody SolicitudB2b body) {
        Resultado r = servicio.recibirAnulacion(token, body);
        return ResponseEntity.status(r.status()).body(r.cuerpo());
    }
}