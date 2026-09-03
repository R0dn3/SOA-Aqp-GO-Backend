package com.integrador.Turismo.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Table(name = "usuarios")
public class Usuario implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "nombre_completo", nullable = false)
    @NotBlank
    private String nombreCompleto;

    @Column(nullable = false, unique = true)
    @Email
    @NotBlank
    private String email;

    @Column(nullable = false)
    private String password;

    private String telefono;

    @Column(name = "dni_pasaporte")
    private String dniPasaporte;

    private String pais;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    // "M", "F", "otro"
    private String genero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Rol rol = Rol.CLIENTE;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()              { return true; }

    public enum Rol {
        CLIENTE,OPERADOR,ADMIN
    }
}
