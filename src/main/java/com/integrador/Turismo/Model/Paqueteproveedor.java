package com.integrador.Turismo.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "paquete_proveedores")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Paqueteproveedor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paquete_id", nullable = false)
    private Paquete paquete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    // Rol específico en este paquete: "TRANSPORTE", "HOTEL", "RESTAURANTE"
    // (puede diferir del tipo del proveedor si es multirol)
    private String rol;

    @Column(columnDefinition = "TEXT")
    private String notas;
}
