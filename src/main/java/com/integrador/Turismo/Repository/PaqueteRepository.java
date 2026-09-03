package com.integrador.Turismo.Repository;

import com.integrador.Turismo.Model.Paquete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PaqueteRepository extends JpaRepository<Paquete,String> {
    List<Paquete> findByActivoTrue();

    @Query("SELECT p FROM Paquete p JOIN p.lugares l WHERE l.id = :lugarId AND p.activo = true")
    List<Paquete> findByLugarId(String lugarId);
}
