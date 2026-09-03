package com.integrador.Turismo.Service;

import com.integrador.Turismo.DTO.*;
import com.integrador.Turismo.Model.Usuario;
import com.integrador.Turismo.Repository.UsuarioRepository;
import com.integrador.Turismo.Security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
@RequiredArgsConstructor

public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;

    public AuthResponse register(RegisterRequest req) {
        if (usuarioRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        Usuario usuario = Usuario.builder()
                .nombreCompleto(req.nombreCompleto())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .telefono(req.telefono())
                .rol(Usuario.Rol.CLIENTE)
                .build();

        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario);
        return new AuthResponse(token, usuario.getEmail(),
                usuario.getNombreCompleto(), usuario.getRol().name());
    }

    public AuthResponse registerAdmin(RegisterRequest req) {
        if (usuarioRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        Usuario usuario = Usuario.builder()
                .nombreCompleto(req.nombreCompleto())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .telefono(req.telefono())
                .rol(req.rol())
                .build();

        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario);
        return new AuthResponse(token, usuario.getEmail(),
                usuario.getNombreCompleto(), usuario.getRol().name());
    }

    public AuthResponse login(LoginRequest req) {
        // Lanza excepción si credenciales incorrectas
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        Usuario usuario = usuarioRepository.findByEmail(req.email())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        String token = jwtService.generateToken(usuario);
        return new AuthResponse(token, usuario.getEmail(),
                usuario.getNombreCompleto(), usuario.getRol().name());
    }
}
