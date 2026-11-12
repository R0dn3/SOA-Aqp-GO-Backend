package com.integrador.Turismo.Mensajeria;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.integrador.Turismo.Model.Notificacion;
import com.integrador.Turismo.Model.Reserva;
import com.integrador.Turismo.Repository.NotificacionRepository;
import com.integrador.Turismo.Repository.ReservaRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Transparencia con el cliente cuando la coordinación con los proveedores se
 * retrasa. Solo avisa cuando el problema deja de ser transitorio (se agotaron
 * los reintentos) y cuando se resuelve. No menciona proveedores (son de uso
 * interno) ni detalles técnicos, y nunca debe romper el flujo de la mensajería.
 */
@Component
@Slf4j
public class AvisoProveedores {

    public static final String TIPO_PENDIENTE = "SERVICIOS_PENDIENTES";
    public static final String TIPO_CONFIRMADO = "SERVICIOS_CONFIRMADOS";

    private final NotificacionRepository notificacionRepository;
    private final ReservaRepository reservaRepository;
    private final TransactionTemplate tx;

    public AvisoProveedores(NotificacionRepository notificacionRepository,
            ReservaRepository reservaRepository, TransactionTemplate tx) {
        this.notificacionRepository = notificacionRepository;
        this.reservaRepository = reservaRepository;
        this.tx = tx;
    }

    /**
     * Se agotaron los reintentos con los proveedores: se tranquiliza al cliente.
     */
    public void avisarPendiente(EventoReserva evento) {
        if (evento.tipo() != EventoReserva.Tipo.RESERVA_CONFIRMADA) {
            return;
        }
        guardar(evento, TIPO_PENDIENTE, false);
    }

    /**
     * La coordinación se completó (reproceso exitoso): se cierra el aviso anterior.
     */
    public void avisarResuelto(EventoReserva evento) {
        if (evento.tipo() != EventoReserva.Tipo.RESERVA_CONFIRMADA) {
            return;
        }
        guardar(evento, TIPO_CONFIRMADO, true);
    }

    private void guardar(EventoReserva evento, String tipo, boolean requierePendiente) {
        try {
            tx.executeWithoutResult(status -> {
                String reservaId = evento.reservaId();

                if (notificacionRepository.existsByReservaIdAndTipo(reservaId, tipo)) {
                    return; // ya se avisó, no se duplica
                }
                if (requierePendiente
                        && !notificacionRepository.existsByReservaIdAndTipo(reservaId, TIPO_PENDIENTE)) {
                    return; // el cliente nunca supo del retraso: no hay nada que cerrar
                }

                Reserva reserva = reservaRepository.findById(reservaId).orElse(null);
                if (reserva == null || reserva.getEstado() != Reserva.Estado.CONFIRMADA) {
                    return; // cancelada u otro estado: el aviso ya no aplica
                }

                String paquete = reserva.getPaquete().getNombre();
                String codigo = reservaId.substring(0, Math.min(8, reservaId.length()));

                String mensaje = TIPO_PENDIENTE.equals(tipo)
                        ? "Tu reserva #" + codigo + " de \"" + paquete + "\" sigue confirmada y tu pago está seguro. "
                                + "Estamos coordinando algunos servicios de tu paquete y te avisaremos apenas queden confirmados."
                        : "Ya confirmamos todos los servicios de tu reserva #" + codigo + " de \"" + paquete + "\".";

                notificacionRepository.save(Notificacion.builder()
                        .usuarioId(evento.usuarioId())
                        .tipo(tipo)
                        .mensaje(mensaje)
                        .reservaId(reservaId)
                        .correlationId(evento.correlationId())
                        .build());

                log.info("[ESB-AVISO] Notificación {} guardada para la reserva {}", tipo, reservaId);
            });
        } catch (RuntimeException e) {
            log.warn("[ESB-AVISO] No se pudo guardar el aviso {} de la reserva {}: {}",
                    tipo, evento.reservaId(), e.getMessage());
        }
    }
}