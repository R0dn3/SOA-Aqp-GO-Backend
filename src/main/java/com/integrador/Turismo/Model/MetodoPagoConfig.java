package com.integrador.Turismo.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "metodos_pago_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetodoPagoConfig {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Pago.Metodo metodo;

    @Column(nullable = false)
    private boolean activo;
}