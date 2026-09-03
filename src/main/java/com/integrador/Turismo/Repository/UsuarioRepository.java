package com.integrador.Turismo.Repository;

import com.integrador.Turismo.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
