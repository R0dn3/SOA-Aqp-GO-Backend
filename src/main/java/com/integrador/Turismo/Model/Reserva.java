package com.integrador.Turismo.Model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paquete_id", nullable = false)
    private Paquete paquete;

    @Column(name = "fecha_salida", nullable = false)
    @NotNull
    private LocalDate fechaSalida;

    @Column(name = "num_personas", nullable = false)
    @Min(1)
    private Integer numPersonas;

    // Se calcula al crear: paquete.precioBase * numPersonas
    @Column(name = "precio_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Estado estado = Estado.PENDIENTE_PAGO;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Acompañantes (personas adicionales además del titular)
    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Acompanante> acompanantes = new ArrayList<>();

    // Pagos asociados a esta reserva
    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Pago> pagos = new ArrayList<>();

    public enum Estado {
        PENDIENTE_PAGO,
        CONFIRMADA,
        CANCELADA,
        COMPLETADA
    }
}
