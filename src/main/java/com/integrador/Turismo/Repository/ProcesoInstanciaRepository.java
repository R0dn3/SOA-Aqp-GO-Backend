package com.integrador.Turismo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.integrador.Turismo.Model.ProcesoInstancia;

public interface ProcesoInstanciaRepository extends JpaRepository<ProcesoInstancia, String> {

    List<ProcesoInstancia> findTop50ByOrderByInicioDesc();

    List<ProcesoInstancia> findTop200ByOrderByInicioDesc();
}