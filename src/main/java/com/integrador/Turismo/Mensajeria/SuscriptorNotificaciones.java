package com.integrador.Turismo.Mensajeria;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Suscriptor de los eventos de reserva. Simula el envío del correo al cliente.
 * Con aqpgo.esb.cola.simular-fallo=true lanza un error a propósito, para
 * demostrar reintentos y dead letter.
 */
@Component
@Slf4j
public class SuscriptorNotificaciones {

    private final boolean simularFallo;
    private final long procesamientoMs;

    public SuscriptorNotificaciones(
            @Value("${aqpgo.esb.cola.simular-fallo:false}") boolean simularFallo,
            @Value("${aqpgo.esb.cola.procesamiento-ms:1000}") long procesamientoMs) {
        this.simularFallo = simularFallo;
        this.procesamientoMs = procesamientoMs;
    }

    public void procesar(EventoReserva evento) {
        log.info("[ESB-SUBSCRIBER] Procesando {} de la reserva {}", evento.tipo(), evento.reservaId());

        // Simula el tiempo que tardaría en enviarse un correo.
        try {
            Thread.sleep(procesamientoMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Procesamiento interrumpido");
        }

        if (simularFallo) {
            throw new IllegalStateException("Servicio de notificaciones no disponible (falla simulada)");
        }

        if (evento.tipo() == EventoReserva.Tipo.RESERVA_CONFIRMADA) {
            log.info("[ESB-SUBSCRIBER] Correo de confirmación enviado al usuario {} (simulado)", evento.usuarioId());
        } else {
            log.info("[ESB-SUBSCRIBER] Aviso de cancelación enviado al usuario {} (simulado)", evento.usuarioId());
        }
    }
}