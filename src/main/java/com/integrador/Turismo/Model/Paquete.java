package com.integrador.Turismo.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paquetes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paquete {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    @NotBlank
    private String nombre;

    private String subtitulo;

    // Todos los campos de texto largo se guardan como Markdown
    @Column(name = "resumen_md", columnDefinition = "TEXT")
    private String resumenMd;

    @Column(name = "recomendaciones_md", columnDefinition = "TEXT")
    private String recomendacionesMd;

    @Column(name = "incluye_md", columnDefinition = "TEXT")
    private String incluyeMd;

    @Column(name = "no_incluye_md", columnDefinition = "TEXT")
    private String noIncluyeMd;

    @Column(name = "preguntas_md", columnDefinition = "TEXT")
    private String preguntasMd;

    @Column(name = "precio_base", nullable = false, precision = 10, scale = 2)
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal precioBase;

    @Column(name = "duracion_dias", nullable = false)
    @Min(1)
    private Integer duracionDias;

    @Column(name = "duracion_noches")
    private Integer duracionNoches;

    @Column(name = "mapa_url", columnDefinition = "TEXT")
    private String mapaUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Relaciones ──────────────────────────────────────────────

    // Lugares del paquete con orden — tabla intermedia paquete_lugar
    @ManyToMany
    @JoinTable(name = "paquete_lugar", joinColumns = @JoinColumn(name = "paquete_id"), inverseJoinColumns = @JoinColumn(name = "lugar_id"))
    @OrderColumn(name = "orden")
    @Builder.Default
    private List<Lugar> lugares = new ArrayList<>();

    // Fotos del paquete
    @OneToMany(mappedBy = "paquete", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<Paquetefoto> fotos = new ArrayList<>();

    // Itinerario por días
    @OneToMany(mappedBy = "paquete", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("diaNumero ASC")
    @Builder.Default
    private List<Itinerariodia> itinerario = new ArrayList<>();

    // Proveedores asignados (solo referencia interna)
    @OneToMany(mappedBy = "paquete", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Paqueteproveedor> proveedores = new ArrayList<>();
}