//Service\B2bDestinosService.java
package com.integrador.Turismo.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.integrador.Turismo.Model.OrdenProveedor;
import com.integrador.Turismo.Model.Paquete;
import com.integrador.Turismo.Model.Paqueteproveedor;
import com.integrador.Turismo.Model.Proveedor;
import com.integrador.Turismo.Model.Reserva;
import com.integrador.Turismo.Repository.OrdenProveedorRepository;
import com.integrador.Turismo.Repository.ReservaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lee (en una transacción corta) los datos que necesita el suscriptor
 * Proveedores, para no mantener la conexión a la base mientras llama por HTTP.
 */
@Service
@RequiredArgsConstructor
public class B2bDestinosService {

    public record Destino(String proveedorId, String nombre, String tipo, String rol) {
    }

    public record ReservaB2b(String reservaId, String paquete, String cliente, String fechaSalida,
            int personas, List<Destino> destinos) {
    }

    private final ReservaRepository reservaRepository;
    private final OrdenProveedorRepository ordenRepository;

    @Transactional(readOnly = true)
    public ReservaB2b cargarReserva(String reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalStateException("Reserva no encontrada: " + reservaId));
        Paquete paquete = reserva.getPaquete();

        Set<String> vistos = new HashSet<>();
        List<Destino> destinos = new ArrayList<>();
        for (Paqueteproveedor pp : paquete.getProveedores()) {
            Proveedor p = pp.getProveedor();
            if (!vistos.add(p.getId())) {
                continue;
            }
            String rol = pp.getRol() != null && !pp.getRol().isBlank() ? pp.getRol() : p.getTipo().name();
            destinos.add(new Destino(p.getId(), p.getNombre(), p.getTipo().name(), rol));
        }

        return new ReservaB2b(reservaId, paquete.getNombre(), reserva.getUsuario().getNombreCompleto(),
                String.valueOf(reserva.getFechaSalida()), reserva.getNumPersonas(), destinos);
    }

    /**
     * Proveedores con una orden ACEPTADA para esa reserva (los que hay que avisar
     * si se cancela).
     */
    public List<OrdenProveedor> ordenesAceptadas(String reservaId) {
        return ordenRepository.findByReservaId(reservaId).stream()
                .filter(o -> "ACEPTADA".equals(o.getEstado()))
                .toList();
    }
}