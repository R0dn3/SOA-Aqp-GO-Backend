package com.integrador.Turismo.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lugares")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Lugar {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    @NotBlank
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String region;

    // Relación inversa — no se mapea a columna
    @JsonIgnore
    @ManyToMany(mappedBy = "lugares")
    @Builder.Default
    private List<Paquete> paquetes = new ArrayList<>();
}
