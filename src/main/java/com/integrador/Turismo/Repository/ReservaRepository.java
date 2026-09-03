package com.integrador.Turismo.Repository;

import com.integrador.Turismo.Model.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva,String> {
    List<Reserva> findByUsuarioId(String usuarioId);
    List<Reserva> findByPaqueteId(String paqueteId);
    List<Reserva> findByEstado(Reserva.Estado estado);

    Page<Reserva> findAll(Pageable pageable);
}
