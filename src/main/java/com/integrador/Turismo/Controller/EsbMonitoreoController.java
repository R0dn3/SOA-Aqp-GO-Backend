package com.integrador.Turismo.Controller;

import com.integrador.Turismo.Model.MensajeRechazado;
import com.integrador.Turismo.Repository.MensajeRechazadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitoreo")
@RequiredArgsConstructor
public class EsbMonitoreoController {

    private final MensajeRechazadoRepository repository;

    @GetMapping("/rechazados")
    public List<MensajeRechazado> rechazados() {
        return repository.findTop50ByOrderByCreatedAtDesc();
    }
}