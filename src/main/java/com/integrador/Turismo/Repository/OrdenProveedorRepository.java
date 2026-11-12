//Repository\OrdenProveedorRepository.java
package com.integrador.Turismo.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.integrador.Turismo.Model.OrdenProveedor;

public interface OrdenProveedorRepository extends JpaRepository<OrdenProveedor, String> {

    Optional<OrdenProveedor> findByClaveIdempotencia(String claveIdempotencia);

    List<OrdenProveedor> findByReservaId(String reservaId);

    // [PROCESOS] varias reservas en una sola consulta (diagrama de coreografía)
    List<OrdenProveedor> findByReservaIdIn(List<String> reservaIds);

    List<OrdenProveedor> findTop100ByOrderByCreadaEnDesc();
}