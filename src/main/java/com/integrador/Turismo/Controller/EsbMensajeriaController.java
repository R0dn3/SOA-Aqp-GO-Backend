package com.integrador.Turismo.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.integrador.Turismo.Mensajeria.ConfigMensajeria;
import com.integrador.Turismo.Mensajeria.SuscriptorAuditoria;
import com.integrador.Turismo.Mensajeria.SuscriptorNotificaciones;
import com.integrador.Turismo.Model.AuditoriaEvento;
import com.integrador.Turismo.Model.MensajeRechazado;
import com.integrador.Turismo.Model.Notificacion;
import com.integrador.Turismo.Repository.AuditoriaEventoRepository;
import com.integrador.Turismo.Repository.MensajeRechazadoRepository;
import com.integrador.Turismo.Repository.NotificacionRepository;
import com.integrador.Turismo.Repository.UsuarioRepository;
import com.integrador.Turismo.Service.ReprocesoService;

import lombok.RequiredArgsConstructor;

/**
 * Panel "Mensajería" del administrador. Todo bajo /api/admin/**, que
 * SecurityConfig ya restringe al rol ADMIN.
 */
@RestController
@RequestMapping("/api/admin/esb")
@RequiredArgsConstructor
public class EsbMensajeriaController {

    private final NotificacionRepository notificacionRepository;
    private final MensajeRechazadoRepository mensajeRepository;
    private final AuditoriaEventoRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReprocesoService reprocesoService;
    private final ConfigMensajeria config;

    // ── DTOs ──────────────────────────────────────────────────────────

    public record EntregaDto(String id, String estado, String suscriptor, String eventoTipo,
            String reservaId, String clienteNombre, String detalle, String correlationId,
            LocalDateTime fecha, boolean reprocesable) {
    }

    public record EntregaStatsDto(long entregadas, long fallidas, long reprocesadas) {
    }

    public record ResultadoSuscriptorDto(String suscriptor, String estado) {
    }

    public record AuditoriaDto(String id, String tipoEvento, String reservaId, String clienteNombre,
            String clienteEmail, String paqueteNombre, String monto, String metodo, String estadoPago,
            String correlationId, LocalDateTime fecha, List<ResultadoSuscriptorDto> resultados) {
    }

    public record ConfigDto(boolean simularFalloNotificaciones) {
    }

    // ── Bloque 1: Notificaciones (entregadas + fallidas) ──────────────

    @GetMapping("/entregas")
    public List<EntregaDto> entregas() {
        List<Notificacion> notificaciones = notificacionRepository.findTop100ByOrderByCreadaEnDesc();
        List<MensajeRechazado> fallos = mensajeRepository.findTop100ByTipoOrderByCreatedAtDesc("FALLO_COLA")
                .stream()
                .filter(f -> f.getSuscriptor() != null) // oculta registros anteriores sin datos del evento
                .toList();

        Set<String> usuarioIds = new HashSet<>();
        notificaciones.forEach(n -> usuarioIds.add(n.getUsuarioId()));
        fallos.forEach(f -> {
            if (f.getUsuarioId() != null) {
                usuarioIds.add(f.getUsuarioId());
            }
        });
        Map<String, String> nombres = nombresDe(usuarioIds);

        List<EntregaDto> lista = new ArrayList<>();

        for (Notificacion n : notificaciones) {
            lista.add(new EntregaDto(n.getId(), "ENTREGADA", SuscriptorNotificaciones.NOMBRE,
                    n.getTipo(), n.getReservaId(), nombres.getOrDefault(n.getUsuarioId(), "—"),
                    n.getMensaje(), n.getCorrelationId(), n.getCreadaEn(), false));
        }

        for (MensajeRechazado f : fallos) {
            boolean reprocesado = "REPROCESADO".equals(f.getEstadoReproceso());
            lista.add(new EntregaDto(f.getId(), reprocesado ? "REPROCESADA" : "FALLIDA",
                    f.getSuscriptor() != null ? f.getSuscriptor() : "—",
                    f.getEventoTipo(), f.getReservaId(),
                    nombres.getOrDefault(f.getUsuarioId(), "—"),
                    f.getMotivo(), f.getCorrelationId(), f.getCreatedAt(),
                    !reprocesado && f.getSuscriptor() != null));
        }

        lista.sort(Comparator.comparing(EntregaDto::fecha, Comparator.reverseOrder()));
        return lista.stream().limit(100).toList();
    }

    @GetMapping("/entregas/stats")
    public EntregaStatsDto stats() {
        return new EntregaStatsDto(
                notificacionRepository.count(),
                mensajeRepository.contarPendientes(),
                mensajeRepository.countByTipoAndEstadoReproceso("FALLO_COLA", "REPROCESADO"));
    }

    @PostMapping("/entregas/{id}/reprocesar")
    public ReprocesoService.Resultado reprocesar(@PathVariable String id) {
        return reprocesoService.reprocesar(id);
    }

    @PostMapping("/entregas/reprocesar-todos")
    public ReprocesoService.Resumen reprocesarTodos() {
        return reprocesoService.reprocesarTodos();
    }

    // ── Interruptor de falla (solo demostración) ──────────────────────

    @GetMapping("/config")
    public ConfigDto getConfig() {
        return new ConfigDto(config.isSimularFalloNotificaciones());
    }

    @PatchMapping("/config")
    public ConfigDto setConfig(@RequestBody ConfigDto body) {
        config.setSimularFalloNotificaciones(body.simularFalloNotificaciones());
        return new ConfigDto(config.isSimularFalloNotificaciones());
    }

    // ── Bloque 2: Auditoría ───────────────────────────────────────────

    @GetMapping("/auditoria")
    public List<AuditoriaDto> auditoria() {
        List<AuditoriaEvento> eventos = auditoriaRepository.findTop100ByOrderByRegistradoEnDesc();
        if (eventos.isEmpty()) {
            return List.of();
        }
        List<String> reservaIds = eventos.stream().map(AuditoriaEvento::getReservaId).distinct().toList();

        // Resultado de Notificaciones por evento: se deduce de lo que existe.
        Set<String> entregadas = notificacionRepository.findByReservaIdIn(reservaIds).stream()
                .map(n -> n.getReservaId() + "|" + n.getTipo())
                .collect(Collectors.toSet());
        Set<String> fallidas = mensajeRepository.findByReservaIdIn(reservaIds).stream()
                .filter(f -> "FALLO_COLA".equals(f.getTipo())
                        && SuscriptorNotificaciones.NOMBRE.equals(f.getSuscriptor()))
                .map(f -> f.getReservaId() + "|" + f.getEventoTipo())
                .collect(Collectors.toSet());

        List<AuditoriaDto> resultado = new ArrayList<>();
        for (AuditoriaEvento e : eventos) {
            String clave = e.getReservaId() + "|" + e.getTipoEvento();
            String estadoNotif = entregadas.contains(clave) ? "ENTREGADA"
                    : fallidas.contains(clave) ? "FALLIDA" : "EN_PROCESO";
            resultado.add(new AuditoriaDto(e.getId(), e.getTipoEvento(), e.getReservaId(),
                    e.getClienteNombre(), e.getClienteEmail(), e.getPaqueteNombre(), e.getMonto(),
                    e.getMetodo(), e.getEstadoPago(), e.getCorrelationId(), e.getRegistradoEn(),
                    List.of(new ResultadoSuscriptorDto(SuscriptorNotificaciones.NOMBRE, estadoNotif),
                            new ResultadoSuscriptorDto(SuscriptorAuditoria.NOMBRE, "REGISTRADA"))));
        }
        return resultado;
    }

    private Map<String, String> nombresDe(Collection<String> ids) {
        Map<String, String> nombres = new HashMap<>();
        usuarioRepository.findAllById(ids).forEach(u -> nombres.put(u.getId(), u.getNombreCompleto()));
        return nombres;
    }
}