package com.integrador.Turismo.Service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.integrador.Turismo.DTO.AuthResponse;
import com.integrador.Turismo.Model.Usuario;
import com.integrador.Turismo.Repository.UsuarioRepository;
import com.integrador.Turismo.Security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {
    private final UsuarioRepository usuarioRepository;
    private final JwtService        jwtService;
    private final PasswordEncoder   passwordEncoder;

    @Transactional
    public AuthResponse autenticarConGoogle(String idToken) {
        try {
            // 1. Verificar el token de Firebase
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);

            String email          = decoded.getEmail();
            String nombreCompleto = decoded.getName();

            // 2. Buscar o crear el usuario en nuestra BD
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseGet(() -> {
                        // Primera vez que inicia con Google — crear cuenta automáticamente
                        Usuario nuevo = Usuario.builder()
                                .nombreCompleto(nombreCompleto != null ? nombreCompleto : email)
                                .email(email)
                                // Password aleatorio — nunca lo usará porque accede con Google
                                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .rol(Usuario.Rol.CLIENTE)
                                .build();
                        return usuarioRepository.save(nuevo);
                    });

            // 3. Generar nuestro propio JWT
            String jwt = jwtService.generateToken(usuario);
            return new AuthResponse(jwt, usuario.getEmail(),
                    usuario.getNombreCompleto(), usuario.getRol().name());

        } catch (Exception e) {
            throw new RuntimeException("Token de Google inválido: " + e.getMessage());
        }
    }
}
