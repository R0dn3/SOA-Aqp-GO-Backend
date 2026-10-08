package com.integrador.Turismo.Repository;

import com.integrador.Turismo.Model.MensajeRechazado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensajeRechazadoRepository extends JpaRepository<MensajeRechazado, String> {
    List<MensajeRechazado> findTop50ByOrderByCreatedAtDesc();
}