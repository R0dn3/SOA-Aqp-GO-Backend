package com.integrador.Turismo.Model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "itinerario_dias")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Itinerariodia {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paquete_id", nullable = false)
    private Paquete paquete;

    @Column(name = "dia_numero", nullable = false)
    @Min(1)
    private Integer diaNumero;

    @Column(nullable = false)
    @NotBlank
    private String titulo;

    // Descripción detallada del día en Markdown
    @Column(name = "descripcion_md", columnDefinition = "TEXT")
    private String descripcionMd;
}
