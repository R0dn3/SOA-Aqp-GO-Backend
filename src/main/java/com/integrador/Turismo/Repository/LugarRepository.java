package com.integrador.Turismo.Repository;

import com.integrador.Turismo.Model.Lugar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LugarRepository extends JpaRepository<Lugar, String> {
}
