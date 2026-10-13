package com.integrador.Turismo.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.integrador.Turismo.Mensajeria.ColaEsb;
import com.integrador.Turismo.Mensajeria.EventoReserva;
import com.integrador.Turismo.Model.MensajeRechazado;
import com.integrador.Turismo.Repository.MensajeRechazadoRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reproceso de mensajes en dead letter (solo FALLO_COLA). Reenvía el evento
 * únicamente al suscriptor que falló. Puede dispararse a mano (admin) o solo,
 * cada cierto tiempo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReprocesoService {

    private final MensajeRechazadoRepository repository;
    private final ColaEsb colaEsb;

    public record Resultado(boolean ok, String mensaje) {
    }

    public record Resumen(int intentados, int exitosos, int fallidos) {
    }

    public Resultado reprocesar(String id) {
        MensajeRechazado m = repository.findById(id).orElse(null);
        if (m == null) {
            return new Resultado(false, "El mensaje ya no existe");
        }
        return intentar(m);
    }

    public Resumen reprocesarTodos() {
        Resumen r = procesarPendientes();
        log.info("[ESB-REPROCESS] Reproceso manual de todos: intentados={}, exitosos={}, fallidos={}",
                r.intentados(), r.exitosos(), r.fallidos());
        return r;
    }

    /** Reproceso automático: revisa los pendientes cada intervalo configurado. */
    @Scheduled(fixedDelayString = "${aqpgo.esb.reproceso.intervalo-ms:180000}", initialDelayString = "${aqpgo.esb.reproceso.intervalo-ms:180000}")
    public void reprocesarAutomatico() {
        List<MensajeRechazado> pendientes = repository.findPendientesReproceso();
        if (pendientes.isEmpty()) {
            return;
        }
        log.info("[ESB-REPROCESS] Reproceso automático: {} mensaje(s) pendiente(s)", pendientes.size());
        Resumen r = procesarPendientes();
        log.info("[ESB-REPROCESS] Reproceso automático terminado: exitosos={}, fallidos={}",
                r.exitosos(), r.fallidos());
    }

    private Resumen procesarPendientes() {
        List<MensajeRechazado> pendientes = repository.findPendientesReproceso();
        int ok = 0;
        for (MensajeRechazado m : pendientes) {
            if (intentar(m).ok()) {
                ok++;
            }
        }
        return new Resumen(pendientes.size(), ok, pendientes.size() - ok);
    }

    private synchronized Resultado intentar(MensajeRechazado recibido) {
        // Se vuelve a leer: otro hilo (manual o automático) pudo resolverlo ya.
        MensajeRechazado m = repository.findById(recibido.getId()).orElse(recibido);

        if ("REPROCESADO".equals(m.getEstadoReproceso())) {
            return new Resultado(true, "Este mensaje ya estaba reprocesado");
        }
        if (!"FALLO_COLA".equals(m.getTipo()) || m.getSuscriptor() == null
                || m.getEventoTipo() == null || m.getReservaId() == null) {
            return new Resultado(false, "Este mensaje no se puede reprocesar (sin datos del evento)");
        }

        EventoReserva evento = new EventoReserva(
                EventoReserva.Tipo.valueOf(m.getEventoTipo()),
                m.getReservaId(),
                m.getUsuarioId(),
                m.getEstadoPago(),
                m.getCorrelationId(),
                m.getCreatedAt());

        String error = colaEsb.reentregar(m.getSuscriptor(), evento);
        m.setReprocesadoEn(LocalDateTime.now());

        if (error == null) {
            m.setEstadoReproceso("REPROCESADO");
            repository.save(m);
            return new Resultado(true, "Reprocesado: " + m.getSuscriptor() + " procesó el evento");
        }

        m.setEstadoReproceso("FALLIDO_DE_NUEVO");
        m.setMotivo(error.length() > 500 ? error.substring(0, 500) : error);
        repository.save(m);
        return new Resultado(false, "Volvió a fallar: " + error);
    }
}