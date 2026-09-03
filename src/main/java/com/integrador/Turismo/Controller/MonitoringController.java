package com.integrador.Turismo.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class MonitoringController {
    @GetMapping("/runtime")
    public String runtimeError() {
        throw new RuntimeException("Error de prueba RuntimeException");
    }

    @GetMapping("/illegal-argument")
    public String illegalArgument() {
        throw new IllegalArgumentException("Parámetro inválido de prueba");
    }

    @GetMapping("/illegal-state")
    public String illegalState() {
        throw new IllegalStateException("Estado inválido de prueba");
    }

    @GetMapping("/security")
    public String security() {
        throw new SecurityException("Error de seguridad de prueba");
    }

}
