//Mensajeria\SuscriptorNotificaciones.java
package com.integrador.Turismo.Mensajeria;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.integrador.Turismo.Model.Notificacion;
import com.integrador.Turismo.Model.Reserva;
import com.integrador.Turismo.Repository.NotificacionRepository;
import com.integrador.Turismo.Repository.ReservaRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Suscriptor 1 (coreografía): avisa al cliente dentro de la web. Guarda una
 * notificación que el cliente ve en la campana. Con el interruptor de falla
 * activo lanza un error a propósito, para demostrar reintentos y dead letter.
 */
@Component
@Slf4j
public class SuscriptorNotificaciones implements SuscriptorEsb {

    public static final String NOMBRE = "Notificaciones";

    private final NotificacionRepository notificacionRepository;
    private final ReservaRepository reservaRepository;
    private final ConfigMensajeria config;
    private final TransactionTemplate tx;
    private final long procesamientoMs;

    public SuscriptorNotificaciones(NotificacionRepository notificacionRepository,
            ReservaRepository reservaRepository,
            ConfigMensajeria config,
            TransactionTemplate tx,
            @Value("${aqpgo.esb.cola.procesamiento-ms:1000}") long procesamientoMs) {
        this.notificacionRepository = notificacionRepository;
        this.reservaRepository = reservaRepository;
        this.config = config;
        this.tx = tx;
        this.procesamientoMs = procesamientoMs;
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public boolean escucha(EventoReserva.Tipo tipo) {
        return true; // confirmaciones y cancelaciones
    }

    @Override
    public void procesar(EventoReserva evento) {
        log.info("[ESB-SUBSCRIBER] [{}] Procesando {} de la reserva {}",
                NOMBRE, evento.tipo(), evento.reservaId());

        // Simula el tiempo que tardaría un servicio externo de avisos.
        try {
            Thread.sleep(procesamientoMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Procesamiento interrumpido");
        }

        if (config.isSimularFalloNotificaciones()) {
            throw new IllegalStateException("Servicio de notificaciones no disponible (falla simulada)");
        }

        tx.executeWithoutResult(status -> guardar(evento));
    }

    private void guardar(EventoReserva evento) {
        String tipo = evento.tipo().name();

        // Evita duplicar el aviso si el mensaje se vuelve a procesar.
        if (notificacionRepository.existsByReservaIdAndTipo(evento.reservaId(), tipo)) {
            log.info("[ESB-SUBSCRIBER] [{}] La notificación de {} ya existe. No se duplica.",
                    NOMBRE, evento.reservaId());
            return;
        }

        Reserva reserva = reservaRepository.findById(evento.reservaId())
                .orElseThrow(() -> new IllegalStateException("Reserva no encontrada: " + evento.reservaId()));

        String paquete = reserva.getPaquete().getNombre();
        String codigo = evento.reservaId().substring(0, Math.min(8, evento.reservaId().length()));

        String mensaje = evento.tipo() == EventoReserva.Tipo.RESERVA_CONFIRMADA
                ? "Tu reserva #" + codigo + " de \"" + paquete + "\" fue confirmada. ¡Buen viaje!"
                : "Tu reserva #" + codigo + " de \"" + paquete + "\" fue cancelada.";

        notificacionRepository.save(Notificacion.builder()
                .usuarioId(evento.usuarioId())
                .tipo(tipo)
                .mensaje(mensaje)
                .reservaId(evento.reservaId())
                .correlationId(evento.correlationId())
                .build());

        log.info("[ESB-SUBSCRIBER] [{}] Notificación guardada para el usuario {} ({})",
                NOMBRE, evento.usuarioId(), tipo);
    }
}