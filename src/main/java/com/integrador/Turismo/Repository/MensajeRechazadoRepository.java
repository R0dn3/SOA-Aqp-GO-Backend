//MensajeRechazadoRepository.java
package com.integrador.Turismo.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.integrador.Turismo.Model.MensajeRechazado;

public interface MensajeRechazadoRepository extends JpaRepository<MensajeRechazado, String> {

    List<MensajeRechazado> findTop50ByOrderByCreatedAtDesc();

    List<MensajeRechazado> findTop100ByTipoOrderByCreatedAtDesc(String tipo);

    List<MensajeRechazado> findByReservaIdIn(Collection<String> reservaIds);

    long countByTipoAndEstadoReproceso(String tipo, String estadoReproceso);

    /** FALLO_COLA aún sin resolver (nunca reprocesado o que volvió a fallar). */
    @Query("select m from MensajeRechazado m "
            + "where m.tipo = 'FALLO_COLA' and m.suscriptor is not null "
            + "and (m.estadoReproceso is null or m.estadoReproceso = 'FALLIDO_DE_NUEVO') "
            + "order by m.createdAt asc")
    List<MensajeRechazado> findPendientesReproceso();

    @Query("select count(m) from MensajeRechazado m "
            + "where m.tipo = 'FALLO_COLA' and m.suscriptor is not null "
            + "and (m.estadoReproceso is null or m.estadoReproceso = 'FALLIDO_DE_NUEVO')")
    long contarPendientes();

    /** Avisos que un cliente aún no recibió (para el banner de Mis reservas). */
    @Query("select m from MensajeRechazado m "
            + "where m.tipo = 'FALLO_COLA' and m.usuarioId = :usuarioId and m.suscriptor = :suscriptor "
            + "and (m.estadoReproceso is null or m.estadoReproceso = 'FALLIDO_DE_NUEVO') "
            + "order by m.createdAt desc")
    List<MensajeRechazado> findPendientesPorUsuario(@Param("usuarioId") String usuarioId,
            @Param("suscriptor") String suscriptor);
}