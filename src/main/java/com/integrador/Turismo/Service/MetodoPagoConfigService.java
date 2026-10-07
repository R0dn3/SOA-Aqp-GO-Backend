package com.integrador.Turismo.Service;

import com.integrador.Turismo.DTO.MetodoPagoConfigDto;
import com.integrador.Turismo.Model.MetodoPagoConfig;
import com.integrador.Turismo.Model.Pago;
import com.integrador.Turismo.Repository.MetodoPagoConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetodoPagoConfigService {

    private final MetodoPagoConfigRepository repo;

    // Todos los métodos con su estado actual (activo=true por defecto si nunca se
    // configuró)
    public List<MetodoPagoConfigDto> listarTodos() {
        return Arrays.stream(Pago.Metodo.values())
                .map(m -> new MetodoPagoConfigDto(
                        m,
                        repo.findById(m).map(MetodoPagoConfig::isActivo).orElse(true)))
                .toList();
    }

    public MetodoPagoConfigDto actualizar(Pago.Metodo metodo, boolean activo) {
        MetodoPagoConfig config = repo.findById(metodo)
                .orElse(MetodoPagoConfig.builder().metodo(metodo).build());
        config.setActivo(activo);
        repo.save(config);
        return new MetodoPagoConfigDto(metodo, activo);
    }

    // Usado por PagoService al momento de procesar el pago
    public boolean estaHabilitado(Pago.Metodo metodo) {
        return repo.findById(metodo).map(MetodoPagoConfig::isActivo).orElse(true);
    }
}