package com.integrador.Turismo.Service;

import com.integrador.Turismo.Model.MensajeRechazado;
import com.integrador.Turismo.Repository.MensajeRechazadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Patrón Dead Letter: guarda los mensajes que el ESB no pudo procesar.
 * Usa una transacción propia (REQUIRES_NEW) para que el registro sobreviva
 * aunque la transacción del orquestador se revierta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterService {

    private final MensajeRechazadoRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String operacion, String tipo, String motivo,
            String contenido, String usuarioId) {
        MensajeRechazado m = MensajeRechazado.builder()
                .correlationId(MDC.get("correlationId"))
                .operacion(operacion)
                .tipo(tipo)
                .motivo(motivo != null && motivo.length() > 500 ? motivo.substring(0, 500) : motivo)
                .contenido(contenido)
                .usuarioId(usuarioId)
                .build();
        repository.save(m);
        log.info("[ESB-DLQ] Mensaje guardado en dead-letter. tipo={}, motivo={}", tipo, motivo);
    }
}