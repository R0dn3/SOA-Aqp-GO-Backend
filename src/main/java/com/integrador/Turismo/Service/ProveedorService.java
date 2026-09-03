package com.integrador.Turismo.Service;

import com.integrador.Turismo.Model.Proveedor;
import com.integrador.Turismo.Repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorService {
    private final ProveedorRepository proveedorRepository;

    public List<Proveedor> listar() {
        return proveedorRepository.findAll();
    }

    public List<Proveedor> listarPorTipo(Proveedor.Tipo tipo) {
        return proveedorRepository.findByTipo(tipo);
    }

    public Proveedor obtenerPorId(String id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado: " + id));
    }

    public Proveedor crear(Proveedor proveedor) {
        return proveedorRepository.save(proveedor);
    }

    public Proveedor actualizar(String id, Proveedor datos) {
        Proveedor proveedor = obtenerPorId(id);
        proveedor.setNombre(datos.getNombre());
        proveedor.setTipo(datos.getTipo());
        proveedor.setTelefono(datos.getTelefono());
        proveedor.setEmail(datos.getEmail());
        proveedor.setNotas(datos.getNotas());
        return proveedorRepository.save(proveedor);
    }

    public void eliminar(String id) {
        proveedorRepository.deleteById(id);
    }
}
