package com.integrador.Turismo.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.integrador.Turismo.Model.ProcesoPaso;

public interface ProcesoPasoRepository extends JpaRepository<ProcesoPaso, String> {

    List<ProcesoPaso> findByInstanciaIdInOrderByOrdenAsc(Collection<String> instanciaIds);
}