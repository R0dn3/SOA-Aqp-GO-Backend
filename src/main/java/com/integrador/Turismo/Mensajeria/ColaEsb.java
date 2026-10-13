//ColaEsb.java
package com.integrador.Turismo.Mensajeria;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 * toma cada evento y lo reparte a TODOS los suscriptores interesados
 * (coreografía). Cada suscriptor trabaja en su propio hilo, con sus propios
 * reintentos y su propio dead letter: si uno falla, los demás no se enteran.
 * No es un broker: si el backend se reinicia, los mensajes pendientes se
 * pierden.
 */
@Component
@Slf4j
public class ColaEsb {

    private static final String OPERACION = "notificarReserva";

    private final BlockingQueue<EventoReserva> cola;
    private final List<SuscriptorEsb> suscriptores;
    private final DeadLetterService deadLetterService;
    private final MeterRegistry meterRegistry;
    private final int maxReintentos;
    private final long esperaBaseMs;

    private Thread consumidor;
    private ExecutorService entregas;
    private volatile boolean activo;

    public ColaEsb(List<SuscriptorEsb> suscriptores,
            DeadLetterService deadLetterService,
            MeterRegistry meterRegistry,
            @Value("${aqpgo.esb.cola.capacidad:100}") int capacidad,
            @Value("${aqpgo.esb.cola.max-reintentos:3}") int maxReintentos,
            @Value("${aqpgo.esb.cola.espera-ms:1000}") long esperaBaseMs) {
        this.suscriptores = suscriptores;
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
        entregas = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "esb-sub");
            t.setDaemon(true);
            return t;
        });
        consumidor = new Thread(this::consumir, "esb-cola");
        consumidor.setDaemon(true);
        consumidor.start();
        log.info("[ESB-QUEUE] Consumidor iniciado (reintentos={}, espera base={} ms, suscriptores={})",
                maxReintentos, esperaBaseMs, suscriptores.stream().map(SuscriptorEsb::nombre).toList());
    }

    @PreDestroy
    void detener() {
        activo = false;
        if (consumidor != null) {
            consumidor.interrupt();
        }
        if (entregas != null) {
            entregas.shutdownNow();
        }
    }

    /**
     * Nunca debe romper el flujo principal: cualquier problema solo se registra.
     */
    public void publicar(EventoReserva evento) {
        try {
            // Se registra ANTES de encolar para que siempre salga antes del [ESB-QUEUE]
            // del consumidor.
            log.info("[ESB-PUBLISH] Publicando evento {} en la cola (reservaId={})",
                    evento.tipo(), evento.reservaId());
            if (cola.offer(evento)) {
                contar(evento, "cola", "publicado");
            } else {
                contar(evento, "cola", "rechazado");
                log.error("[ESB-QUEUE] Cola llena: evento {} de la reserva {} descartado",
                        evento.tipo(), evento.reservaId());
                registrarDeadLetter("TODOS", evento, "Cola llena");
            }
        } catch (RuntimeException e) {
            log.error("[ESB-PUBLISH] No se pudo publicar el evento: {}", e.getMessage());
        }
    }

    /**
     * Reintenta una entrega que antes terminó en dead letter. La llama el
     * reproceso (automático o manual). Devuelve null si salió bien, o el motivo
     * del error. "TODOS" reenvía a todos los suscriptores interesados.
     */
    public String reentregar(String nombreSuscriptor, EventoReserva evento) {
        List<SuscriptorEsb> destino = suscriptores.stream()
                .filter(s -> "TODOS".equals(nombreSuscriptor)
                        ? s.escucha(evento.tipo())
                        : s.nombre().equals(nombreSuscriptor))
                .toList();
        if (destino.isEmpty()) {
            return "Suscriptor desconocido: " + nombreSuscriptor;
        }

        // Se conserva el correlationId de quien llama (p. ej. la petición del admin).
        String previo = MDC.get("correlationId");
        if (evento.correlationId() != null) {
            MDC.put("correlationId", evento.correlationId());
        }
        String actual = nombreSuscriptor;
        try {
            for (SuscriptorEsb s : destino) {
                actual = s.nombre();
                log.info("[ESB-REPROCESS] Reenviando {} de la reserva {} a [{}]",
                        evento.tipo(), evento.reservaId(), actual);
                s.procesar(evento);
                contar(evento, actual, "reprocesado");
                log.info("[ESB-REPROCESS] [{}] procesó el evento correctamente", actual);
            }
            return null;
        } catch (RuntimeException e) {
            contar(evento, actual, "reproceso_fallido");
            log.warn("[ESB-REPROCESS] [{}] volvió a fallar: {}", actual, e.getMessage());
            return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        } finally {
            if (previo != null) {
                MDC.put("correlationId", previo);
            } else {
                MDC.remove("correlationId");
            }
        }
    }

    private void consumir() {
        while (activo) {
            try {
                EventoReserva evento = cola.take();
                repartir(evento);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                log.error("[ESB-QUEUE] Error inesperado en el consumidor: {}", e.getMessage());
            }
        }
    }

    /** Entrega el evento a cada suscriptor interesado, cada uno en su hilo. */
    private void repartir(EventoReserva evento) {
        // El correlationId vive en el hilo de la petición; aquí se vuelve a poner.
        if (evento.correlationId() != null) {
            MDC.put("correlationId", evento.correlationId());
        }
        try {
            List<SuscriptorEsb> destino = suscriptores.stream()
                    .filter(s -> s.escucha(evento.tipo()))
                    .toList();
            log.info("[ESB-QUEUE] Evento {} tomado de la cola (reservaId={}). Se entrega a {} suscriptor(es): {}",
                    evento.tipo(), evento.reservaId(), destino.size(),
                    destino.stream().map(SuscriptorEsb::nombre).toList());

            for (SuscriptorEsb s : destino) {
                entregas.submit(() -> entregar(s, evento));
            }
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Entrega a UN suscriptor, con reintentos. Si agota los intentos, va al dead
     * letter.
     */
    private void entregar(SuscriptorEsb suscriptor, EventoReserva evento) {
        if (evento.correlationId() != null) {
            MDC.put("correlationId", evento.correlationId());
        }
        try {
            for (int intento = 1; intento <= maxReintentos; intento++) {
                try {
                    suscriptor.procesar(evento);
                    contar(evento, suscriptor.nombre(), "procesado");
                    return;
                } catch (RuntimeException e) {
                    log.warn("[ESB-RETRY] [{}] Intento {}/{} falló: {}",
                            suscriptor.nombre(), intento, maxReintentos, e.getMessage());
                    if (intento < maxReintentos) {
                        contar(evento, suscriptor.nombre(), "reintento");
                        try {
                            Thread.sleep(esperaBaseMs * intento);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    } else {
                        contar(evento, suscriptor.nombre(), "rechazado");
                        registrarDeadLetter(suscriptor.nombre(), evento, e.getMessage());
                    }
                }
            }
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void contar(EventoReserva evento, String suscriptor, String resultado) {
        meterRegistry.counter("aqpgo.esb.cola.mensajes",
                "evento", evento.tipo().name(),
                "suscriptor", suscriptor,
                "resultado", resultado).increment();
    }

    private void registrarDeadLetter(String suscriptor, EventoReserva evento, String motivo) {
        try {
            deadLetterService.registrarFalloCola(OPERACION, suscriptor, evento,
                    motivo != null ? motivo : "sin detalle");
        } catch (RuntimeException e) {
            log.error("[ESB-DLQ] No se pudo guardar el mensaje en dead-letter: {}", e.getMessage());
        }
    }
}