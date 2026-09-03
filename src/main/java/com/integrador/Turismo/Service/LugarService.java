package com.integrador.Turismo.Service;

import com.integrador.Turismo.Model.Lugar;
import com.integrador.Turismo.Repository.LugarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LugarService {
    private final LugarRepository lugarRepository;

    public List<Lugar> listar() {
        return lugarRepository.findAll();
    }

    public Lugar obtenerPorId(String id) {
        return lugarRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lugar no encontrado: " + id));
    }

    public Lugar crear(Lugar lugar) {
        return lugarRepository.save(lugar);
    }

    public Lugar actualizar(String id, Lugar datos) {
        Lugar lugar = obtenerPorId(id);
        lugar.setNombre(datos.getNombre());
        lugar.setDescripcion(datos.getDescripcion());
        lugar.setRegion(datos.getRegion());
        return lugarRepository.save(lugar);
    }

    public void eliminar(String id) {
        lugarRepository.deleteById(id);
    }
}
