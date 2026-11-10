package com.integrador.Turismo.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.integrador.Turismo.Model.ProcesoInstancia;
import com.integrador.Turismo.Model.ProcesoPaso;
import com.integrador.Turismo.Repository.ProcesoInstanciaRepository;
import com.integrador.Turismo.Repository.ProcesoPasoRepository;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Motor de seguimiento BPM: el orquestador va anotando cada paso del proceso
 * en una Traza (en memoria). Al terminar, la traza se guarda en la base de
 * datos desde un hilo aparte: la reserva no espera esos inserts y, si algo
 * falla aquí, solo se registra en el log (nunca rompe un pago).
 */
@Service
@Slf4j
public class ProcesoTracker {

    private final ProcesoInstanciaRepository instanciaRepository;
    private final ProcesoPasoRepository pasoRepository;
    private final TransactionTemplate tx;
    private final ExecutorService escritor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "proceso-tracker");
        t.setDaemon(true);
        return t;
    });

    public ProcesoTracker(ProcesoInstanciaRepository instanciaRepository,
            ProcesoPasoRepository pasoRepository,
            PlatformTransactionManager transactionManager) {
        this.instanciaRepository = instanciaRepository;
        this.pasoRepository = pasoRepository;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @PreDestroy
    void detener() {
        escritor.shutdown();
    }

    public Traza iniciar(String proceso, String usuarioId) {
        return new Traza(proceso, usuarioId, org.slf4j.MDC.get("correlationId"));
    }

    /** Cierra la traza y la guarda sin bloquear. Nunca lanza excepciones. */
    public void guardar(Traza traza) {
        try {
            traza.cerrar();
            escritor.execute(() -> persistir(traza));
        } catch (RuntimeException e) {
            log.warn("[BPM] No se pudo programar el guardado de la traza: {}", e.getMessage());
        }
    }

    private void persistir(Traza t) {
        try {
            tx.executeWithoutResult(status -> {
                ProcesoInstancia inst = instanciaRepository.save(ProcesoInstancia.builder()
                        .proceso(t.proceso)
                        .usuarioId(t.usuarioId)
                        .reservaId(t.reservaId)
                        .correlationId(t.correlationId)
                        .estado(t.estado != null ? t.estado : "FALLIDO")
                        .mensaje(t.mensaje)
                        .inicio(t.inicio)
                        .duracionMs(t.duracionMs)
                        .build());
                List<ProcesoPaso> filas = t.pasos.stream()
                        .map(p -> ProcesoPaso.builder()
                                .instanciaId(inst.getId())
                                .orden(p.orden())
                                .paso(p.paso())
                                .estado(p.estado())
                                .duracionMs(p.duracionMs())
                                .detalle(p.detalle())
                                .build())
                        .toList();
                pasoRepository.saveAll(filas);
            });
            log.info("[BPM] Instancia guardada: estado={}, pasos={}, {} ms",
                    t.estado, t.pasos.size(), t.duracionMs);
        } catch (RuntimeException e) {
            log.warn("[BPM] No se pudo guardar la traza del proceso: {}", e.getMessage());
        }
    }

    // ── Traza (objeto de un solo hilo, vive durante una ejecución) ──────

    public record PasoTraza(int orden, String paso, String estado, long duracionMs, String detalle) {
    }

    public static class Traza {
        private final String proceso;
        private final String usuarioId;
        private final String correlationId;
        private final LocalDateTime inicio = LocalDateTime.now();
        private final long inicioNs = System.nanoTime();
        private long ultimoNs = inicioNs;
        private final List<PasoTraza> pasos = new ArrayList<>();
        private String reservaId;
        private String estado;
        private String mensaje;
        private long duracionMs;

        Traza(String proceso, String usuarioId, String correlationId) {
            this.proceso = proceso;
            this.usuarioId = usuarioId;
            this.correlationId = correlationId;
        }

        /** Anota un paso. La duración es el tiempo desde el paso anterior. */
        public void paso(String paso, String estado, String detalle) {
            try {
                long ahora = System.nanoTime();
                pasos.add(new PasoTraza(pasos.size() + 1, paso, estado,
                        (ahora - ultimoNs) / 1_000_000, recortar(detalle)));
                ultimoNs = ahora;
            } catch (RuntimeException e) {
                // la traza nunca debe romper el proceso
            }
        }

        public void reserva(String reservaId) {
            this.reservaId = reservaId;
        }

        public void estado(String estado, String mensaje) {
            this.estado = estado;
            this.mensaje = recortar(mensaje);
        }

        /** Excepción no prevista: si nadie fijó un estado final, queda FALLIDO. */
        public void excepcion(String motivo) {
            if (estado == null) {
                paso("ESB-ERROR", "ERROR", motivo);
                estado("FALLIDO", motivo);
            }
        }

        void cerrar() {
            duracionMs = (System.nanoTime() - inicioNs) / 1_000_000;
        }

        private static String recortar(String s) {
            if (s == null) {
                return null;
            }
            return s.length() > 480 ? s.substring(0, 480) : s;
        }
    }
}