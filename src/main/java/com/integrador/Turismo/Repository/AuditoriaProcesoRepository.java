package com.integrador.Turismo.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.integrador.Turismo.Model.AuditoriaEvento;

public interface AuditoriaProcesoRepository extends JpaRepository<AuditoriaEvento, String> {

    /**
     * Devuelve "reservaId|tipoEvento" de las reservas indicadas (una sola
     * consulta).
     */
    @Query("select concat(a.reservaId, '|', a.tipoEvento) from AuditoriaEvento a where a.reservaId in :ids")
    List<String> claves(@Param("ids") Collection<String> ids);
}