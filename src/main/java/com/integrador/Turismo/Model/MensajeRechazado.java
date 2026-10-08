//Model/MensajeRechazado.java
package com.integrador.Turismo.Model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "esb_mensajes_rechazados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeRechazado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(nullable = false, length = 60)
    private String operacion;

    /** VALIDACION | FALLO_TECNICO | FALLO_COLA */
    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "usuario_id", length = 64)
    private String usuarioId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}