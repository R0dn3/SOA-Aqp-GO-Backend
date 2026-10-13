//PagoRepository.java
package com.integrador.Turismo.Repository;

import com.integrador.Turismo.Model.Pago;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, String> {
    List<Pago> findByReservaId(String reservaId);

    List<Pago> findAll(Sort sort);

    Optional<Pago> findFirstByReferenciaAndEstado(String referencia, Pago.Estado estado);
}