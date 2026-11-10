package com.integrador.Turismo.Model;

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
 * Un paso de una instancia del proceso (ESB-VALIDATE, ESB-OPERATE...).
 * Estados: OK | ERROR | COMPENSADO | OMITIDO.
 */
@Entity
@Table(name = "proceso_pasos", indexes = @Index(name = "idx_paso_instancia", columnList = "instancia_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesoPaso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "instancia_id", nullable = false, length = 64)
    private String instanciaId;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, length = 40)
    private String paso;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "duracion_ms", nullable = false)
    private Long duracionMs;

    @Column(length = 500)
    private String detalle;
}