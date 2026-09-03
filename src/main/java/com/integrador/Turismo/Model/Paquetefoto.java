package com.integrador.Turismo.Model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "paquete_fotos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Paquetefoto {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paquete_id", nullable = false)
    private Paquete paquete;

    @Column(nullable = false)
    @NotBlank
    private String url;         // URL de Cloudinary

    private String alt;         // Texto alternativo para accesibilidad

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;
}
