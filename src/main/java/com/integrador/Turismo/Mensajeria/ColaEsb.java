//ColaEsb.java
package com.integrador.Turismo.Mensajeria;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.integrador.Turismo.Service.DeadLetterService;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Mensajería del ESB (pieza 5). Dos modos, según aqpgo.esb.broker:
 * - "rabbit": publica el evento en RabbitMQ (exchange fanout, una cola por
 * suscriptor). Cada suscriptor lo consume en su propio hilo (coreografía).
 * - "memoria": cola en memoria con un hilo consumidor que reparte a todos los
 * suscriptores interesados.
 * En ambos modos cada suscriptor tiene sus propios reintentos y su propio dead
 * letter: si uno falla, los demás no se enteran.
 */
@Component
@Slf4j
public class ColaEsb {

    private static final String OPERACION = "notificarReserva";

    private final BlockingQueue<EventoReserva> cola;
    private final List<SuscriptorEsb> suscriptores;
    private final DeadLetterService deadLetterService;
    private final MeterRegistry meterRegistry;
    private final ObjectProvider<RabbitTemplate> rabbitTemplate;
    private final boolean modoBroker;
    private final int maxReintentos;
    private final long esperaBaseMs;

    private Thread consumidor;
    private ExecutorService entregas;
    private volatile boolean activo;

    public ColaEsb(List<SuscriptorEsb> suscriptores,
            DeadLetterService deadLetterService,
            MeterRegistry meterRegistry,
            ObjectProvider<RabbitTemplate> rabbitTemplate,
            @Value("${aqpgo.esb.broker:memoria}") String broker,
            @Value("${aqpgo.esb.cola.capacidad:100}") int capacidad,
            @Value("${aqpgo.esb.cola.max-reintentos:3}") int maxReintentos,
            @Value("${aqpgo.esb.cola.espera-ms:1000}") long esperaBaseMs) {
        this.suscriptores = suscriptores;
        this.deadLetterService = deadLetterService;
        this.meterRegistry = meterRegistry;
        this.rabbitTemplate = rabbitTemplate;
        this.modoBroker = "rabbit".equalsIgnoreCase(broker);
        this.maxReintentos = maxReintentos;
        this.esperaBaseMs = esperaBaseMs;
        this.cola = new LinkedBlockingQueue<>(capacidad);
        Gauge.builder("aqpgo.esb.cola.pendientes", cola, BlockingQueue::size).register(meterRegistry);
    }

    @PostConstruct
    void iniciar() {
        activo = true;
        if (modoBroker) {
            log.info(
                    "[ESB-QUEUE] Modo BROKER (RabbitMQ): exchange={}, reintentos={}, espera base={} ms, suscriptores={}",
                    RabbitConfig.EXCHANGE, maxReintentos, esperaBaseMs,
                    suscriptores.stream().map(SuscriptorEsb::nombre).toList());
            return;
        }
        entregas = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "esb-sub");
            t.setDaemon(true);
            return t;
        });
        consumidor = new Thread(this::consumir, "esb-cola");
        consumidor.setDaemon(true);
        consumidor.start();
        log.info("[ESB-QUEUE] Modo MEMORIA: consumidor iniciado (reintentos={}, espera base={} ms, suscriptores={})",
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
            // Se registra ANTES de enviar para que siempre salga antes del [ESB-QUEUE].
            log.info("[ESB-PUBLISH] Publicando evento {} en {} (reservaId={})",
                    evento.tipo(), modoBroker ? "RabbitMQ" : "la cola", evento.reservaId());
            if (modoBroker) {
                publicarEnBroker(evento);
                return;
            }
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

    private void publicarEnBroker(EventoReserva evento) {
        RabbitTemplate template = rabbitTemplate.getIfAvailable();
        if (template == null) {
            contar(evento, "broker", "rechazado");
            registrarDeadLetter("TODOS", evento, "RabbitMQ no está configurado");
            return;
        }
        try {
            template.convertAndSend(RabbitConfig.EXCHANGE, "", EventoJson.escribir(evento));
            contar(evento, "broker", "publicado");
        } catch (RuntimeException e) {
            contar(evento, "broker", "rechazado");
            log.error("[ESB-QUEUE] RabbitMQ no disponible: evento {} de la reserva {} no se pudo publicar: {}",
                    evento.tipo(), evento.reservaId(), e.getMessage());
            registrarDeadLetter("TODOS", evento, "RabbitMQ no disponible: " + e.getMessage());
        }
    }

    /**
     * Lo llama el consumidor de RabbitMQ de cada suscriptor cuando recibe un
     * mensaje de su cola. Aplica los reintentos y, si se agotan, el dead letter.
     * Nunca lanza excepciones: así el mensaje se confirma (ack) y no se reencola.
     */
    public void recibirDelBroker(String nombreSuscriptor, String cuerpo) {
        EventoReserva evento;
        try {
            evento = EventoJson.leer(cuerpo);
        } catch (RuntimeException e) {
            log.error("[ESB-QUEUE] Mensaje ilegible en la cola de [{}], se descarta: {}",
                    nombreSuscriptor, e.getMessage());
            return;
        }

        SuscriptorEsb suscriptor = suscriptores.stream()
                .filter(s -> s.nombre().equals(nombreSuscriptor))
                .findFirst()
                .orElse(null);
        if (suscriptor == null) {
            log.error("[ESB-QUEUE] No existe el suscriptor [{}]; mensaje descartado", nombreSuscriptor);
            return;
        }
        if (!suscriptor.escucha(evento.tipo())) {
            return;
        }

        if (evento.correlationId() != null) {
            MDC.put("correlationId", evento.correlationId());
        }
        try {
            log.info("[ESB-QUEUE] Evento {} recibido de RabbitMQ por [{}] (reservaId={})",
                    evento.tipo(), nombreSuscriptor, evento.reservaId());
            entregar(suscriptor, evento);
        } finally {
            MDC.remove("correlationId");
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

    /**
     * Modo memoria: entrega el evento a cada suscriptor interesado, cada uno en su
     * hilo.
     */
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