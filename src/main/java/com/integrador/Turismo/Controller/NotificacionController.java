package com.integrador.Turismo.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.integrador.Turismo.Mensajeria.SuscriptorNotificaciones;
import com.integrador.Turismo.Model.Notificacion;
import com.integrador.Turismo.Model.Usuario;
import com.integrador.Turismo.Repository.MensajeRechazadoRepository;
import com.integrador.Turismo.Repository.NotificacionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Notificaciones del cliente autenticado (la campana de la barra superior).
 * Siempre se filtra por el usuario del JWT: nadie ve las de otro.
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionRepository repository;
    private final MensajeRechazadoRepository mensajeRepository;

    public record NotificacionDto(String id, String tipo, String mensaje, String reservaId,
            boolean leida, LocalDateTime creadaEn) {
        static NotificacionDto de(Notificacion n) {
            return new NotificacionDto(n.getId(), n.getTipo(), n.getMensaje(), n.getReservaId(),
                    Boolean.TRUE.equals(n.getLeida()), n.getCreadaEn());
        }
    }

    public record AvisoPendienteDto(String reservaId, String eventoTipo) {
    }

    @GetMapping
    public List<NotificacionDto> listar(@AuthenticationPrincipal Usuario usuario) {
        return repository.findTop30ByUsuarioIdOrderByCreadaEnDesc(usuario.getId())
                .stream().map(NotificacionDto::de).toList();
    }

    @GetMapping("/no-leidas")
    public Map<String, Long> noLeidas(@AuthenticationPrincipal Usuario usuario) {
        return Map.of("total", repository.countByUsuarioIdAndLeidaFalse(usuario.getId()));
    }

    /**
     * Avisos de reserva que fallaron y todavía no se reenvían (banner "aviso
     * pendiente").
     */
    @GetMapping("/pendientes")
    public List<AvisoPendienteDto> pendientes(@AuthenticationPrincipal Usuario usuario) {
        return mensajeRepository
                .findPendientesPorUsuario(usuario.getId(), SuscriptorNotificaciones.NOMBRE)
                .stream()
                .filter(m -> m.getReservaId() != null)
                .map(m -> new AvisoPendienteDto(m.getReservaId(), m.getEventoTipo()))
                .toList();
    }

    @PatchMapping("/{id}/leida")
    @Transactional
    public ResponseEntity<Void> marcarLeida(@PathVariable String id,
            @AuthenticationPrincipal Usuario usuario) {
        repository.findByIdAndUsuarioId(id, usuario.getId()).ifPresent(n -> n.setLeida(true));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/leidas")
    @Transactional
    public ResponseEntity<Void> marcarTodasLeidas(@AuthenticationPrincipal Usuario usuario) {
        repository.findByUsuarioIdAndLeidaFalse(usuario.getId()).forEach(n -> n.setLeida(true));
        return ResponseEntity.noContent().build();
    }
}