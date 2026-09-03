package com.integrador.Turismo.Model;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "acompanantes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Acompanante {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @Column(name = "nombre_completo", nullable = false)
    @NotBlank
    private String nombreCompleto;

    @Column(name = "dni_pasaporte", nullable = false)
    @NotBlank
    private String dniPasaporte;

    private String pais;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    // "M", "F", "otro"
    private String genero;

    // Campo libre para alergias, necesidades especiales, etc.
    @Column(name = "datos_adicionales", columnDefinition = "TEXT")
    private String datosAdicionales;
}
