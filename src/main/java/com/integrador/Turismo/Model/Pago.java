package com.integrador.Turismo.Model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Metodo metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Estado estado = Estado.PENDIENTE;

    // Número de operación, ID de transacción, etc.
    private String referencia;

    @Column(name = "fecha_pago")
    @Builder.Default
    private LocalDateTime fechaPago = LocalDateTime.now();

    public enum Metodo {
        TRANSFERENCIA,
        TARJETA,
        YAPE,
        PLIN,
        EFECTIVO
    }

    public enum Estado {
        PENDIENTE,
        VERIFICADO,
        RECHAZADO
    }
}
