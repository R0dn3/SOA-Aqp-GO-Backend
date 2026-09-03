package com.integrador.Turismo.Controller;

import com.integrador.Turismo.DTO.ActualizarPerfilRequest;
import com.integrador.Turismo.DTO.AuthResponse;
import com.integrador.Turismo.DTO.RegisterRequest;
import com.integrador.Turismo.DTO.UsuarioResponse;
import com.integrador.Turismo.Model.Usuario;
import com.integrador.Turismo.Repository.UsuarioRepository;
import com.integrador.Turismo.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;

    // GET /api/usuarios/perfil
    // El cliente ve y puede actualizar su propio perfil
    @GetMapping("/perfil")
    public ResponseEntity<Usuario> perfil(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(usuario);
    }

    // PUT /api/usuarios/perfil
    // Actualiza datos personales del usuario logueado
    @PutMapping("/perfil")
    public ResponseEntity<UsuarioResponse> actualizarPerfil(
            @AuthenticationPrincipal Usuario usuario,
            @RequestBody ActualizarPerfilRequest datos) {

        if (datos.nombreCompleto() != null && !datos.nombreCompleto().isBlank())
            usuario.setNombreCompleto(datos.nombreCompleto());
        if (datos.telefono() != null)
            usuario.setTelefono(datos.telefono());
        if (datos.dniPasaporte() != null)
            usuario.setDniPasaporte(datos.dniPasaporte());
        if (datos.pais() != null)
            usuario.setPais(datos.pais());
        if (datos.fechaNacimiento() != null)
            usuario.setFechaNacimiento(datos.fechaNacimiento());
        if (datos.genero() != null)
            usuario.setGenero(datos.genero());

        usuarioRepository.save(usuario);

        // Devuelve UsuarioResponse — nunca expone el password
        return ResponseEntity.ok(UsuarioResponse.from(usuario));
    }

    // ── Solo ADMIN ────────────────────────────────────────────

    // GET /api/usuarios
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(usuarioRepository.findAll());
    }

    // GET /api/usuarios/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Usuario> obtener(@PathVariable String id) {
        return usuarioRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PATCH /api/usuarios/{id}/rol
    // Admin cambia el rol de un usuario
    // Body: "ADMIN" | "CLIENTE"
    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Usuario> cambiarRol(
            @PathVariable String id,
            @RequestBody Usuario.Rol nuevoRol) {

        return usuarioRepository.findById(id).map(u -> {
            u.setRol(nuevoRol);
            return ResponseEntity.ok(usuarioRepository.save(u));
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/usuarios/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        usuarioRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/usuarios/register
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.registerAdmin(req));
    }
}
