//Repository\NotificacionRepository.java
package com.integrador.Turismo.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.integrador.Turismo.Model.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, String> {

    List<Notificacion> findTop30ByUsuarioIdOrderByCreadaEnDesc(String usuarioId);

    List<Notificacion> findTop100ByOrderByCreadaEnDesc();

    long countByUsuarioIdAndLeidaFalse(String usuarioId);

    List<Notificacion> findByUsuarioIdAndLeidaFalse(String usuarioId);

    List<Notificacion> findByReservaIdIn(Collection<String> reservaIds);

    Optional<Notificacion> findByIdAndUsuarioId(String id, String usuarioId);

    boolean existsByReservaIdAndTipo(String reservaId, String tipo);
}