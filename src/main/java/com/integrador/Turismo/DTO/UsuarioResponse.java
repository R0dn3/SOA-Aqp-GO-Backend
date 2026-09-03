package com.integrador.Turismo.DTO;


import com.integrador.Turismo.Model.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UsuarioResponse(
        String id,
        String nombreCompleto,
        String email,
        String telefono,
        String dniPasaporte,
        String pais,
        LocalDate fechaNacimiento,
        String genero,
        String rol,
        LocalDateTime createdAt)
{
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNombreCompleto(),
                u.getEmail(),
                u.getTelefono(),
                u.getDniPasaporte(),
                u.getPais(),
                u.getFechaNacimiento(),
                u.getGenero(),
                u.getRol().name(),
                u.getCreatedAt()
        );
    }
}
