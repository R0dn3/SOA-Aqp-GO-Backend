package com.integrador.Turismo.Controller;

import com.integrador.Turismo.DTO.*;
import com.integrador.Turismo.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.integrador.Turismo.DTO.GoogleAuthRequest;
import com.integrador.Turismo.Service.GoogleAuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginGoogle(
            @Valid @RequestBody GoogleAuthRequest req) {
        return ResponseEntity.ok(googleAuthService.autenticarConGoogle(req.idToken()));
    }

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
