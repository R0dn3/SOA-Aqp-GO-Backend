package com.integrador.Turismo.Mensajeria;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.integrador.Turismo.Service.DeadLetterService;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Cola en memoria del ESB (pieza 5: mensajería). Un hilo consumidor
 * ("esb-cola")
 * toma los eventos y los entrega al suscriptor, con reintentos y dead letter.
 * No es un broker: si el backend se reinicia, los mensajes pendientes se
 * pierden.
 */
@Component
@Slf4j
public class ColaEsb {

    private static final String OPERACION = "notificarReserva";

    private final BlockingQueue<EventoReserva> cola;
    private final SuscriptorNotificaciones suscriptor;
    private final DeadLetterService deadLetterService;
    private final MeterRegistry meterRegistry;
    private final int maxReintentos;
    private final long esperaBaseMs;

    private Thread consumidor;
    private volatile boolean activo;

    public ColaEsb(SuscriptorNotificaciones suscriptor,
            DeadLetterService deadLetterService,
            MeterRegistry meterRegistry,
            @Value("${aqpgo.esb.cola.capacidad:100}") int capacidad,
            @Value("${aqpgo.esb.cola.max-reintentos:3}") int maxReintentos,
            @Value("${aqpgo.esb.cola.espera-ms:1000}") long esperaBaseMs) {
        this.suscriptor = suscriptor;
        this.deadLetterService = deadLetterService;
        this.meterRegistry = meterRegistry;
        this.maxReintentos = maxReintentos;
        this.esperaBaseMs = esperaBaseMs;
        this.cola = new LinkedBlockingQueue<>(capacidad);
        Gauge.builder("aqpgo.esb.cola.pendientes", cola, BlockingQueue::size).register(meterRegistry);
    }

    @PostConstruct
    void iniciar() {
        activo = true;
        consumidor = new Thread(this::consumir, "esb-cola");
        consumidor.setDaemon(true);
        consumidor.start();
        log.info("[ESB-QUEUE] Consumidor iniciado (reintentos={}, espera base={} ms)", maxReintentos, esperaBaseMs);
    }

    @PreDestroy
    void detener() {
        activo = false;
        if (consumidor != null) {
            consumidor.interrupt();
        }
    }

    /**
     * Nunca debe romper el flujo principal: cualquier problema solo se registra.
     */
    public void publicar(EventoReserva evento) {
        try {
            if (cola.offer(evento)) {
                contar(evento, "publicado");
                log.info("[ESB-PUBLISH] Evento {} encolado (reservaId={}, pendientes={})",
                        evento.tipo(), evento.reservaId(), cola.size());
            } else {
                contar(evento, "rechazado");
                log.error("[ESB-QUEUE] Cola llena: evento {} de la reserva {} descartado",
                        evento.tipo(), evento.reservaId());
                registrarDeadLetter(evento, "Cola llena");
            }
        } catch (RuntimeException e) {
            log.error("[ESB-PUBLISH] No se pudo publicar el evento: {}", e.getMessage());
        }
    }

    private void consumir() {
        while (activo) {
            try {
                EventoReserva evento = cola.take();
                procesar(evento);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                log.error("[ESB-QUEUE] Error inesperado en el consumidor: {}", e.getMessage());
            }
        }
    }

    private void procesar(EventoReserva evento) throws InterruptedException {
        // El correlationId vive en el hilo de la petición; aquí se vuelve a poner.
        if (evento.correlationId() != null) {
            MDC.put("correlationId", evento.correlationId());
        }
        try {
            log.info("[ESB-QUEUE] Evento {} tomado de la cola (reservaId={})",
                    evento.tipo(), evento.reservaId());

            for (int intento = 1; intento <= maxReintentos; intento++) {
                try {
                    suscriptor.procesar(evento);
                    contar(evento, "procesado");
                    return;
                } catch (RuntimeException e) {
                    log.warn("[ESB-RETRY] Intento {}/{} falló: {}", intento, maxReintentos, e.getMessage());
                    if (intento < maxReintentos) {
                        contar(evento, "reintento");
                        Thread.sleep(esperaBaseMs * intento);
                    } else {
                        contar(evento, "rechazado");
                        registrarDeadLetter(evento, e.getMessage());
                    }
                }
            }
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void contar(EventoReserva evento, String resultado) {
        meterRegistry.counter("aqpgo.esb.cola.mensajes",
                "evento", evento.tipo().name(), "resultado", resultado).increment();
    }

    private void registrarDeadLetter(EventoReserva evento, String motivo) {
        try {
            String contenido = "tipo=" + evento.tipo()
                    + ", reservaId=" + evento.reservaId()
                    + ", estadoPago=" + evento.estadoPago()
                    + ", creadoEn=" + evento.creadoEn();
            deadLetterService.registrar(OPERACION, "FALLO_COLA",
                    motivo != null ? motivo : "sin detalle", contenido, evento.usuarioId());
        } catch (RuntimeException e) {
            log.error("[ESB-DLQ] No se pudo guardar el mensaje en dead-letter: {}", e.getMessage());
        }
    }
}