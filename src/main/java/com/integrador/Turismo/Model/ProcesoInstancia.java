package com.integrador.Turismo.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una ejecución del proceso reservarYPagar (instancia BPM).
 * Estados: COMPLETADO | COMPENSADO | RECHAZADO | FALLIDO | REPETIDO.
 */
@Entity
@Table(name = "proceso_instancias", indexes = @Index(name = "idx_proceso_inicio", columnList = "inicio"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesoInstancia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 60)
    private String proceso;

    @Column(name = "usuario_id", length = 64)
    private String usuarioId;

    @Column(name = "reserva_id", length = 64)
    private String reservaId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(length = 500)
    private String mensaje;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(name = "duracion_ms", nullable = false)
    private Long duracionMs;
}