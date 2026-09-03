package com.integrador.Turismo.Service;
import com.integrador.Turismo.DTO.PaqueteCreateDto;
import com.integrador.Turismo.DTO.PaqueteDetalleDto;
import com.integrador.Turismo.DTO.PaqueteResumenDto;
import com.integrador.Turismo.Model.*;
import com.integrador.Turismo.Repository.LugarRepository;
import com.integrador.Turismo.Repository.PaqueteRepository;
import com.integrador.Turismo.Repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor

public class PaqueteService {
    private final PaqueteRepository    paqueteRepository;
    private final LugarRepository      lugarRepository;
    private final ProveedorRepository  proveedorRepository;

    // ── Públicos ──────────────────────────────────────────────

    public List<PaqueteResumenDto> listarActivos() {
        return paqueteRepository.findByActivoTrue()
                .stream()
                .map(PaqueteResumenDto::from)
                .toList();
    }

    public PaqueteDetalleDto obtenerDetalle(String id) {
        return PaqueteDetalleDto.from(obtenerEntidad(id));
    }

    // ── Admin: Crear ──────────────────────────────────────────

    @Transactional
    public PaqueteDetalleDto crear(PaqueteCreateDto dto) {
        Paquete paquete = new Paquete();
        aplicarDatos(paquete, dto);
        return PaqueteDetalleDto.from(paqueteRepository.save(paquete));
    }

    // ── Admin: Editar (reemplaza todo) ────────────────────────

    @Transactional
    public PaqueteDetalleDto actualizar(String id, PaqueteCreateDto dto) {
        Paquete paquete = obtenerEntidad(id);

        // Limpia colecciones — orphanRemoval elimina de BD automáticamente
        paquete.getFotos().clear();
        paquete.getItinerario().clear();
        paquete.getProveedores().clear();
        paquete.getLugares().clear();

        // Flush para que DELETE ocurra antes de INSERT
        paqueteRepository.saveAndFlush(paquete);

        aplicarDatos(paquete, dto);
        return PaqueteDetalleDto.from(paqueteRepository.save(paquete));
    }

    // ── Admin: Toggle activo ──────────────────────────────────

    @Transactional
    public PaqueteDetalleDto toggleActivo(String id) {
        Paquete paquete = obtenerEntidad(id);
        paquete.setActivo(!paquete.getActivo());
        return PaqueteDetalleDto.from(paqueteRepository.save(paquete));
    }

    // ── Admin: Eliminar ───────────────────────────────────────

    @Transactional
    public void eliminar(String id) {
        paqueteRepository.deleteById(id);
    }

    // ── Lógica compartida crear/editar ────────────────────────

    private void aplicarDatos(Paquete paquete, PaqueteCreateDto dto) {

        // Campos simples
        paquete.setNombre(dto.nombre());
        paquete.setSubtitulo(dto.subtitulo());
        paquete.setResumenMd(dto.resumenMd());
        paquete.setRecomendacionesMd(dto.recomendacionesMd());
        paquete.setIncluyeMd(dto.incluyeMd());
        paquete.setNoIncluyeMd(dto.noIncluyeMd());
        paquete.setPreguntasMd(dto.preguntasMd());
        paquete.setPrecioBase(dto.precioBase());
        paquete.setDuracionDias(dto.duracionDias());
        paquete.setDuracionNoches(dto.duracionNoches());
        paquete.setMapaUrl(dto.mapaUrl());

        // Lugares — valida que todos existan
        if (dto.lugarIds() != null && !dto.lugarIds().isEmpty()) {
            List<Lugar> lugares = lugarRepository.findAllById(dto.lugarIds());
            if (lugares.size() != dto.lugarIds().size()) {
                throw new RuntimeException("Uno o más lugares no existen");
            }
            paquete.getLugares().addAll(lugares);
        }

        // Proveedores — busca entidad y asigna con rol y notas
        if (dto.proveedores() != null) {
            for (PaqueteCreateDto.ProveedorAsignadoDto p : dto.proveedores()) {
                Proveedor proveedor = proveedorRepository.findById(p.proveedorId())
                        .orElseThrow(() -> new RuntimeException(
                                "Proveedor no encontrado: " + p.proveedorId()));

                paquete.getProveedores().add(Paqueteproveedor.builder()
                        .paquete(paquete)
                        .proveedor(proveedor)
                        .rol(p.rol())
                        .notas(p.notas())
                        .build());
            }
        }

        // Fotos — URLs ya subidas a Cloudinary
        if (dto.fotos() != null) {
            for (int i = 0; i < dto.fotos().size(); i++) {
                PaqueteCreateDto.FotoDto f = dto.fotos().get(i);
                paquete.getFotos().add(Paquetefoto.builder()
                        .paquete(paquete)
                        .url(f.url())
                        .alt(f.alt())
                        .orden(f.orden() != null ? f.orden() : i)
                        .build());
            }
        }

        // Itinerario — un objeto por día
        if (dto.itinerario() != null) {
            dto.itinerario().forEach(d ->
                    paquete.getItinerario().add(Itinerariodia.builder()
                            .paquete(paquete)
                            .diaNumero(d.diaNumero())
                            .titulo(d.titulo())
                            .descripcionMd(d.descripcionMd())
                            .build())
            );
        }
    }

    public Paquete obtenerEntidad(String id) {
        return paqueteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paquete no encontrado: " + id));
    }
}
