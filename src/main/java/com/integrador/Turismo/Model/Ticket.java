package com.integrador.Turismo.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tickets", indexes = {
        // Índices para las queries más frecuentes — evita full table scans
        @Index(name = "idx_ticket_usuario",  columnList = "usuario_id"),
        @Index(name = "idx_ticket_estado",   columnList = "estado"),
        @Index(name = "idx_ticket_created",  columnList = "created_at"),
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Usuario que creó el ticket — LAZY para no cargar datos innecesarios
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Reserva relacionada — opcional, LAZY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Column(nullable = false)
    @NotBlank
    private String asunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Estado estado = Estado.ABIERTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Prioridad prioridad = Prioridad.MEDIA;

    // Admin asignado — opcional, LAZY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Usuario adminAsignado;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Mensajes — LAZY: solo se cargan cuando se pide el detalle
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<TicketMensaje> mensajes = new ArrayList<>();

    // Actualiza updatedAt automáticamente
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Enums ─────────────────────────────────────────────────

    public enum Tipo {
        SERVICIO,       // cambio de fecha, agregar persona, factura
        INFORMACION,    // dudas sobre el tour
        ACCESO          // permisos especiales
    }

    public enum Estado {
        ABIERTO,
        EN_PROCESO,
        RESUELTO,
        CERRADO
    }

    public enum Prioridad {
        BAJA,
        MEDIA,
        ALTA
    }
}
