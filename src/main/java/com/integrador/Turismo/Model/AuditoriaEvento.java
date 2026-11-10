//Model\AuditoriaEvento.java
package com.integrador.Turismo.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro permanente de cada evento de reserva. Guarda una copia (snapshot)
 * de los datos al momento del evento: si luego cambia el paquete o el usuario,
 * la auditoría conserva lo que pasó.
 */
@Entity
@Table(name = "auditoria_eventos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tipo_evento", nullable = false, length = 30)
    private String tipoEvento;

    @Column(name = "reserva_id", nullable = false, length = 64)
    private String reservaId;

    @Column(name = "usuario_id", length = 64)
    private String usuarioId;

    @Column(name = "cliente_nombre")
    private String clienteNombre;

    @Column(name = "cliente_email")
    private String clienteEmail;

    @Column(name = "paquete_nombre")
    private String paqueteNombre;

    @Column(length = 30)
    private String monto;

    @Column(length = 30)
    private String metodo;

    @Column(name = "estado_pago", length = 30)
    private String estadoPago;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "registrado_en", nullable = false)
    @Builder.Default
    private LocalDateTime registradoEn = LocalDateTime.now();
}