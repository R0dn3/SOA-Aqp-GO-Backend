//Model\OrdenProveedor.java
package com.integrador.Turismo.Model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Orden de servicio B2B recibida por un proveedor (su bandeja de entrada).
 * Estados: ACEPTADA | RECHAZADA | ANULADA.
 * La clave de idempotencia (reservaId:proveedorId) evita órdenes duplicadas.
 */
@Entity
@Table(name = "ordenes_proveedor", uniqueConstraints = @UniqueConstraint(name = "uk_orden_clave", columnNames = "clave_idempotencia"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "reserva_id", nullable = false, length = 64)
    private String reservaId;

    @Column(name = "proveedor_id", nullable = false, length = 64)
    private String proveedorId;

    @Column(name = "proveedor_nombre")
    private String proveedorNombre;

    @Column(name = "tipo_proveedor", length = 30)
    private String tipoProveedor;

    @Column(length = 40)
    private String rol;

    @Column(name = "clave_idempotencia", nullable = false, length = 140)
    private String claveIdempotencia;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(length = 300)
    private String motivo;

    @Column(length = 300)
    private String detalle;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "creada_en", nullable = false)
    @Builder.Default
    private LocalDateTime creadaEn = LocalDateTime.now();

    @Column(name = "actualizada_en", nullable = false)
    @Builder.Default
    private LocalDateTime actualizadaEn = LocalDateTime.now();
}
