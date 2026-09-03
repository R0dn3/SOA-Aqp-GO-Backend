package com.integrador.Turismo.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_mensajes", indexes = {
        @Index(name = "idx_mensaje_ticket", columnList = "ticket_id"),
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TicketMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Ticket al que pertenece — LAZY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    // Quien escribió — LAZY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank
    private String mensaje;

    // true = respuesta del admin, false = mensaje del cliente
    @Column(name = "es_admin", nullable = false)
    @Builder.Default
    private boolean esAdmin = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
