package com.integrador.Turismo.Model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "proveedores")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Proveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    @NotBlank
    private String nombre;

    // "TRANSPORTE", "HOTEL", "RESTAURANTE"
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    private String telefono;
    private String email;

    @Column(columnDefinition = "TEXT")
    private String notas;

    public enum Tipo {
        TRANSPORTE, HOTEL, RESTAURANTE
    }
}
