//SuscriptorAuditoria.java
package com.integrador.Turismo.Mensajeria;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.integrador.Turismo.Model.AuditoriaEvento;
import com.integrador.Turismo.Model.Pago;
import com.integrador.Turismo.Model.Reserva;
import com.integrador.Turismo.Model.Usuario;
import com.integrador.Turismo.Repository.AuditoriaEventoRepository;
import com.integrador.Turismo.Repository.PagoRepository;
import com.integrador.Turismo.Repository.ReservaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Suscriptor 2 (coreografía): deja un registro permanente de cada evento, con
 * una copia de cliente, paquete, monto y método al momento en que ocurrió.
 * Es independiente de Notificaciones: si uno falla, el otro sigue.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SuscriptorAuditoria implements SuscriptorEsb {

    public static final String NOMBRE = "Auditoría";

    private final AuditoriaEventoRepository auditoriaRepository;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public boolean escucha(EventoReserva.Tipo tipo) {
        return true;
    }

    @Override
    @Transactional
    public void procesar(EventoReserva evento) {
        log.info("[ESB-SUBSCRIBER] [{}] Registrando {} de la reserva {}",
                NOMBRE, evento.tipo(), evento.reservaId());

        String tipo = evento.tipo().name();
        if (auditoriaRepository.existsByReservaIdAndTipoEvento(evento.reservaId(), tipo)) {
            log.info("[ESB-SUBSCRIBER] [{}] El evento ya estaba registrado. No se duplica.", NOMBRE);
            return;
        }

        Reserva reserva = reservaRepository.findById(evento.reservaId())
                .orElseThrow(() -> new IllegalStateException("Reserva no encontrada: " + evento.reservaId()));
        Usuario cliente = reserva.getUsuario();

        // Pago de referencia: el verificado si existe; si no, el último intento.
        List<Pago> pagos = pagoRepository.findByReservaId(reserva.getId());
        Pago pago = pagos.stream()
                .filter(p -> p.getEstado() == Pago.Estado.VERIFICADO)
                .findFirst()
                .orElse(pagos.isEmpty() ? null : pagos.get(pagos.size() - 1));

        auditoriaRepository.save(AuditoriaEvento.builder()
                .tipoEvento(tipo)
                .reservaId(evento.reservaId())
                .usuarioId(evento.usuarioId())
                .clienteNombre(cliente.getNombreCompleto())
                .clienteEmail(cliente.getEmail())
                .paqueteNombre(reserva.getPaquete().getNombre())
                .monto(pago != null ? String.valueOf(pago.getMonto()) : null)
                .metodo(pago != null ? String.valueOf(pago.getMetodo()) : null)
                .estadoPago(evento.estadoPago())
                .correlationId(evento.correlationId())
                .build());

        log.info("[ESB-SUBSCRIBER] [{}] Evento {} registrado en auditoría", NOMBRE, tipo);
    }
}