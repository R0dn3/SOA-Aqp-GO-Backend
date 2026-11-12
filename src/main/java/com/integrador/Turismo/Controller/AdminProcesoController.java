//Controller\AdminProcesoController
package com.integrador.Turismo.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.integrador.Turismo.Model.MensajeRechazado;
import com.integrador.Turismo.Model.OrdenProveedor;
import com.integrador.Turismo.Model.ProcesoInstancia;
import com.integrador.Turismo.Model.ProcesoPaso;
import com.integrador.Turismo.Repository.AuditoriaProcesoRepository;
import com.integrador.Turismo.Repository.MensajeRechazadoRepository;
import com.integrador.Turismo.Repository.NotificacionRepository;
import com.integrador.Turismo.Repository.OrdenProveedorRepository;
import com.integrador.Turismo.Repository.ProcesoInstanciaRepository;
import com.integrador.Turismo.Repository.ProcesoPasoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seguimiento de procesos (BPM): instancias del proceso reservarYPagar con sus
 * pasos, el resultado de la coreografía (Notificaciones, Auditoría y
 * Proveedores
 * B2B) y la definición BPMN descargable (v1 y v2).
 * Solo ADMIN (SecurityConfig protege /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/procesos")
@RequiredArgsConstructor
public class AdminProcesoController {

        private final ProcesoInstanciaRepository instanciaRepository;
        private final ProcesoPasoRepository pasoRepository;
        private final AuditoriaProcesoRepository auditoriaRepository;
        private final MensajeRechazadoRepository rechazadoRepository;
        private final NotificacionRepository notificacionRepository;
        private final OrdenProveedorRepository ordenProveedorRepository; // [B2B]

        public record PasoDto(int orden, String paso, String estado, long duracionMs, String detalle) {
        }

        // [B2B] se agrega "proveedores"
        public record CoreografiaDto(String auditoria, String notificaciones, String proveedores,
                        boolean brokerFallo) {
        }

        public record InstanciaDto(String id, String proceso, String estado, String reservaId,
                        String correlationId, String usuarioId, LocalDateTime inicio, long duracionMs,
                        String mensaje, List<PasoDto> pasos, CoreografiaDto coreografia) {
        }

        @GetMapping
        public List<InstanciaDto> listar() {
                List<ProcesoInstancia> instancias = instanciaRepository.findTop50ByOrderByInicioDesc();
                if (instancias.isEmpty()) {
                        return List.of();
                }

                List<String> ids = instancias.stream().map(ProcesoInstancia::getId).toList();
                Map<String, List<ProcesoPaso>> pasosPorInstancia = pasoRepository
                                .findByInstanciaIdInOrderByOrdenAsc(ids).stream()
                                .collect(Collectors.groupingBy(ProcesoPaso::getInstanciaId));

                List<String> reservaIds = instancias.stream()
                                .map(ProcesoInstancia::getReservaId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList();
                Set<String> auditados = reservaIds.isEmpty()
                                ? Set.of()
                                : new HashSet<>(auditoriaRepository.claves(reservaIds));
                Set<String> entregadas = reservaIds.isEmpty()
                                ? Set.of()
                                : notificacionRepository.findByReservaIdIn(reservaIds).stream()
                                                .map(n -> n.getReservaId() + "|" + n.getTipo())
                                                .collect(Collectors.toSet());
                List<MensajeRechazado> pendientes = reservaIds.isEmpty()
                                ? List.of()
                                : rechazadoRepository.findByReservaIdIn(reservaIds).stream()
                                                .filter(AdminProcesoController::estaPendiente)
                                                .toList();
                // [B2B] órdenes de proveedor por reserva (una sola consulta)
                Map<String, List<OrdenProveedor>> ordenes = reservaIds.isEmpty()
                                ? Map.of()
                                : ordenProveedorRepository.findByReservaIdIn(reservaIds).stream()
                                                .collect(Collectors.groupingBy(OrdenProveedor::getReservaId));

                return instancias.stream()
                                .map(i -> new InstanciaDto(
                                                i.getId(), i.getProceso(), i.getEstado(), i.getReservaId(),
                                                i.getCorrelationId(), i.getUsuarioId(), i.getInicio(),
                                                i.getDuracionMs(),
                                                i.getMensaje(),
                                                pasosPorInstancia.getOrDefault(i.getId(), List.of()).stream()
                                                                .map(p -> new PasoDto(p.getOrden(), p.getPaso(),
                                                                                p.getEstado(),
                                                                                p.getDuracionMs(), p.getDetalle()))
                                                                .toList(),
                                                coreografia(i, auditados, entregadas, pendientes, ordenes)))
                                .toList();
        }

        /**
         * Descarga la definición BPMN 2.0 del proceso.
         * version=2 (por defecto): coreografía con 3 suscriptores. version=1: la
         * original.
         */
        @GetMapping("/definicion/bpmn")
        public ResponseEntity<byte[]> definicionBpmn(
                        @RequestParam(name = "version", defaultValue = "2") int version) throws IOException {
                int v = version == 1 ? 1 : 2;
                String archivo = "reservar_y_pagar.v" + v + ".bpmn";
                byte[] xml = new ClassPathResource("bpmn/" + archivo).getInputStream().readAllBytes();
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo + "\"")
                                .contentType(MediaType.APPLICATION_XML)
                                .body(xml);
        }

        // ── Coreografía: qué pasó con el evento después de publicarse ───────

        private static boolean estaPendiente(MensajeRechazado m) {
                return "FALLO_COLA".equals(m.getTipo())
                                && (m.getEstadoReproceso() == null
                                                || "FALLIDO_DE_NUEVO".equals(m.getEstadoReproceso()));
        }

        private static boolean afecta(MensajeRechazado m, String reservaId, String suscriptor) {
                return reservaId.equals(m.getReservaId())
                                && (suscriptor.equals(m.getSuscriptor()) || "TODOS".equals(m.getSuscriptor()));
        }

        private CoreografiaDto coreografia(ProcesoInstancia i, Set<String> auditados,
                        Set<String> entregadas, List<MensajeRechazado> pendientes,
                        Map<String, List<OrdenProveedor>> ordenes) {
                boolean confirmado = "COMPLETADO".equals(i.getEstado());
                boolean compensado = "COMPENSADO".equals(i.getEstado());
                String reservaId = i.getReservaId();
                if (reservaId == null || !(confirmado || compensado)) {
                        return new CoreografiaDto("NO_APLICA", "NO_APLICA", "NO_APLICA", false);
                }

                String tipoEvento = confirmado ? "RESERVA_CONFIRMADA" : "RESERVA_CANCELADA";
                String clave = reservaId + "|" + tipoEvento;
                boolean reciente = i.getInicio().isAfter(LocalDateTime.now().minusMinutes(2));

                boolean falloAuditoria = pendientes.stream().anyMatch(m -> afecta(m, reservaId, "Auditoría"));
                boolean falloNotif = pendientes.stream().anyMatch(m -> afecta(m, reservaId, "Notificaciones"));
                boolean falloProv = pendientes.stream().anyMatch(m -> afecta(m, reservaId, "Proveedores")); // [B2B]
                boolean brokerFallo = pendientes.stream()
                                .anyMatch(m -> reservaId.equals(m.getReservaId()) && "TODOS".equals(m.getSuscriptor()));

                String auditoria = auditados.contains(clave) ? "REGISTRADA"
                                : falloAuditoria ? "FALLIDA"
                                                : reciente ? "EN_PROCESO" : "SIN_REGISTRO";
                String notificaciones = entregadas.contains(clave) ? "ENTREGADA"
                                : falloNotif ? "FALLIDA"
                                                : reciente ? "EN_PROCESO" : "SIN_REGISTRO";

                // [B2B] Proveedores: solo actúa cuando la reserva se confirmó (envía ORDERS)
                String proveedores;
                if (!confirmado) {
                        proveedores = "NO_APLICA";
                } else {
                        List<OrdenProveedor> lista = ordenes.getOrDefault(reservaId, List.of());
                        boolean hayRechazo = lista.stream().anyMatch(o -> "RECHAZADA".equals(o.getEstado()));
                        if (falloProv || hayRechazo) {
                                proveedores = "FALLIDA";
                        } else if (!lista.isEmpty()) {
                                proveedores = "ENTREGADA";
                        } else {
                                proveedores = reciente ? "EN_PROCESO" : "SIN_ORDENES";
                        }
                }

                return new CoreografiaDto(auditoria, notificaciones, proveedores, brokerFallo);
        }
}