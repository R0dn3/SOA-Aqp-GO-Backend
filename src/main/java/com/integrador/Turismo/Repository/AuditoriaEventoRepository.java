package com.integrador.Turismo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.integrador.Turismo.Model.AuditoriaEvento;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, String> {

    boolean existsByReservaIdAndTipoEvento(String reservaId, String tipoEvento);

    List<AuditoriaEvento> findTop100ByOrderByRegistradoEnDesc();
}