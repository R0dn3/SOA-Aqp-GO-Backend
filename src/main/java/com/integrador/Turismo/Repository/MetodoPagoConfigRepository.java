package com.integrador.Turismo.Repository;

import com.integrador.Turismo.Model.MetodoPagoConfig;
import com.integrador.Turismo.Model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetodoPagoConfigRepository extends JpaRepository<MetodoPagoConfig, Pago.Metodo> {
}