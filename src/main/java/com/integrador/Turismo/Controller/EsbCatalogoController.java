package com.integrador.Turismo.Controller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

/**
 * Catálogo de servicios del bus (registro tipo UDDI + gobierno SOA).
 * Descubre en tiempo de ejecución los servicios REST y SOAP realmente
 * desplegados y les agrega metadatos de gobierno (tipo, capa, versión, dueño,
 * acceso, SLA) y la latencia real medida con Micrometer desde el último
 * arranque. Solo ADMIN (SecurityConfig protege /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/esb/catalogo")
@RequiredArgsConstructor
public class EsbCatalogoController {

    private static final String VERSION = "1.0";
    private static final String PROPIETARIO = "Equipo AqpGo";
    private static final int SLA_PROCESO_MS = 2000;
    private static final int SLA_SERVICIO_MS = 1500;

    private final ApplicationContext context;
    private final MeterRegistry meterRegistry;

    // ── Gobierno: tipo y capa por servicio (documental; lo no listado usa valores
    // por defecto) ──

    private record Meta(String tipo, String capa, String descripcion) {
    }

    private static final Meta POR_DEFECTO = new Meta("TAREA", "Servicios de negocio",
            "Servicio de la plataforma AQP Go");

    private static final Map<String, Meta> META = Map.ofEntries(
            Map.entry("OrquestadorEndpoint", new Meta("PROCESO", "Procesos (orquestación)",
                    "Compone Reservar + Pagar con compensación (Saga) y publica el evento")),
            Map.entry("ReservaEndpoint", new Meta("ENTIDAD", "Servicios de negocio", "Reservas (contrato SOAP)")),
            Map.entry("ReporteEndpoint", new Meta("UTILIDAD", "Servicios de apoyo", "Reportes (contrato SOAP)")),
            Map.entry("ReservaController", new Meta("ENTIDAD", "Servicios de negocio", "Reservas")),
            Map.entry("PagoController", new Meta("ENTIDAD", "Servicios de negocio", "Pagos")),
            Map.entry("PaqueteController", new Meta("ENTIDAD", "Servicios de negocio", "Paquetes turísticos")),
            Map.entry("LugarController", new Meta("ENTIDAD", "Servicios de negocio", "Lugares")),
            Map.entry("ProveedorController", new Meta("ENTIDAD", "Servicios de negocio", "Proveedores")),
            Map.entry("UsuarioController", new Meta("ENTIDAD", "Servicios de negocio", "Usuarios")),
            Map.entry("TicketController", new Meta("ENTIDAD", "Servicios de negocio", "Tickets de soporte")),
            Map.entry("AuthController", new Meta("UTILIDAD", "Seguridad", "Autenticación y registro")),
            Map.entry("NotificacionController",
                    new Meta("UTILIDAD", "Servicios de apoyo", "Notificaciones al cliente")),
            Map.entry("ReporteController", new Meta("UTILIDAD", "Servicios de apoyo", "Reportes")),
            Map.entry("BackupController", new Meta("UTILIDAD", "Infraestructura", "Respaldo de datos")),
            Map.entry("MonitoringController", new Meta("UTILIDAD", "Infraestructura", "Monitoreo del sistema")),
            Map.entry("AdminController", new Meta("TAREA", "Administración", "Indicadores y gestión del panel admin")),
            Map.entry("MetodoPagoAdminController", new Meta("TAREA", "Administración", "Métodos de pago habilitados")),
            Map.entry("EsbMensajeriaController",
                    new Meta("UTILIDAD", "Bus de servicios (ESB)", "Mensajería, auditoría y reproceso")),
            Map.entry("EsbMonitoreoController",
                    new Meta("UTILIDAD", "Bus de servicios (ESB)", "Mensajes rechazados (dead letter)")),
            Map.entry("EsbRegistroController",
                    new Meta("UTILIDAD", "Bus de servicios (ESB)", "Registro de contratos y operaciones SOAP")),
            Map.entry("EsbCatalogoController",
                    new Meta("UTILIDAD", "Bus de servicios (ESB)", "Catálogo de servicios y gobierno SOA")),
            Map.entry("AdminProcesoController",
                    new Meta("UTILIDAD", "Bus de servicios (ESB)", "Seguimiento de procesos (BPM)")),
            Map.entry("AdminBamController",
                    new Meta("UTILIDAD", "Bus de servicios (ESB)", "Monitoreo de actividad del negocio (BAM)")));

    public record ServicioDto(String id, String nombre, String tipo, String capa, String protocolo,
            String version, String propietario, String estado, String acceso, String descripcion,
            int slaMs, List<String> endpoints, long llamadas, long promedioMs, long maxMs,
            long errores, String cumplimientoSla) {
    }

    public record ContratoDto(String nombre, String wsdl) {
    }

    public record CatalogoDto(List<ServicioDto> servicios, List<ContratoDto> contratos) {
    }

    @GetMapping
    public CatalogoDto catalogo() {
        List<ServicioDto> servicios = new ArrayList<>();

        // ── REST ────────────────────────────────────────────────
        for (Object bean : context.getBeansWithAnnotation(RestController.class).values()) {
            Class<?> clazz = AopUtils.getTargetClass(bean);
            RequestMapping claseRm = AnnotatedElementUtils.findMergedAnnotation(clazz, RequestMapping.class);
            List<String> bases = rutas(claseRm);

            List<String> endpoints = new ArrayList<>();
            Set<String> plantillas = new HashSet<>();
            for (Method m : clazz.getDeclaredMethods()) {
                RequestMapping rm = AnnotatedElementUtils.findMergedAnnotation(m, RequestMapping.class);
                if (rm == null) {
                    continue;
                }
                String verbo = rm.method().length == 0 ? "ANY"
                        : Arrays.stream(rm.method()).map(RequestMethod::name).collect(Collectors.joining("|"));
                for (String base : bases) {
                    for (String sub : rutas(rm)) {
                        String ruta = unir(base, sub);
                        endpoints.add(verbo + " " + ruta);
                        plantillas.add(ruta);
                    }
                }
            }
            endpoints.sort(Comparator.naturalOrder());

            Stats s = estadisticasHttp(plantillas);
            String primera = endpoints.isEmpty() ? "" : endpoints.get(0).substring(endpoints.get(0).indexOf(' ') + 1);
            servicios.add(armar(clazz.getSimpleName(), "REST", accesoRest(primera), endpoints, s, false));
        }

        // ── SOAP ────────────────────────────────────────────────
        for (Object bean : context.getBeansWithAnnotation(Endpoint.class).values()) {
            Class<?> clazz = AopUtils.getTargetClass(bean);
            List<String> endpoints = new ArrayList<>();
            for (Method m : clazz.getDeclaredMethods()) {
                PayloadRoot root = AnnotatedElementUtils.findMergedAnnotation(m, PayloadRoot.class);
                if (root != null) {
                    endpoints.add("SOAP " + root.localPart());
                }
            }
            endpoints.sort(Comparator.naturalOrder());

            boolean orquestador = "OrquestadorEndpoint".equals(clazz.getSimpleName());
            Stats s = orquestador ? estadisticasOrquestador() : new Stats(0, 0, 0, 0);
            servicios.add(armar(clazz.getSimpleName(), "SOAP",
                    "Público a nivel HTTP; autenticación propia SOAP (SoapAuth)", endpoints, s, orquestador));
        }

        servicios.sort(Comparator.comparing(ServicioDto::capa).thenComparing(ServicioDto::nombre));

        List<ContratoDto> contratos = context.getBeansOfType(DefaultWsdl11Definition.class)
                .keySet().stream()
                .sorted()
                .map(n -> new ContratoDto(n, "/ws/" + n + ".wsdl"))
                .toList();

        return new CatalogoDto(servicios, contratos);
    }

    // ── Armado de cada servicio ─────────────────────────────────

    private ServicioDto armar(String clase, String protocolo, String acceso, List<String> endpoints,
            Stats s, boolean proceso) {
        Meta meta = META.getOrDefault(clase, POR_DEFECTO);
        int sla = "PROCESO".equals(meta.tipo()) ? SLA_PROCESO_MS : SLA_SERVICIO_MS;
        long promedio = s.llamadas() == 0 ? 0 : Math.round(s.totalMs() / s.llamadas());
        String cumplimiento = s.llamadas() == 0 ? "SIN_DATOS" : promedio <= sla ? "CUMPLE" : "NO_CUMPLE";
        String version = proceso ? VERSION + " (BPMN v1)" : VERSION;
        return new ServicioDto(clase.toLowerCase(), clase, meta.tipo(), meta.capa(), protocolo, version,
                PROPIETARIO, "ACTIVO", acceso, meta.descripcion(), sla, endpoints,
                s.llamadas(), promedio, Math.round(s.maxMs()), s.errores(), cumplimiento);
    }

    // ── Latencia real (Micrometer, desde el último arranque) ────

    private record Stats(long llamadas, double totalMs, double maxMs, long errores) {
    }

    private Stats estadisticasHttp(Set<String> plantillas) {
        long llamadas = 0;
        long errores = 0;
        double total = 0;
        double max = 0;
        for (Timer t : meterRegistry.find("http.server.requests").timers()) {
            String uri = t.getId().getTag("uri");
            if (uri != null && plantillas.contains(uri)) {
                llamadas += t.count();
                total += t.totalTime(TimeUnit.MILLISECONDS);
                max = Math.max(max, t.max(TimeUnit.MILLISECONDS));
                if ("SERVER_ERROR".equals(t.getId().getTag("outcome"))) {
                    errores += t.count();
                }
            }
        }
        return new Stats(llamadas, total, max, errores);
    }

    private Stats estadisticasOrquestador() {
        long llamadas = 0;
        double total = 0;
        double max = 0;
        for (Timer t : meterRegistry.find("aqpgo.esb.orquestacion.duracion").timers()) {
            llamadas += t.count();
            total += t.totalTime(TimeUnit.MILLISECONDS);
            max = Math.max(max, t.max(TimeUnit.MILLISECONDS));
        }
        long errores = Math.round(meterRegistry.find("aqpgo.esb.mensajes")
                .tag("resultado", "fallo_tecnico").counters().stream()
                .mapToDouble(c -> c.count()).sum());
        return new Stats(llamadas, total, max, errores);
    }

    // ── Utilidades de rutas y acceso ────────────────────────────

    private static List<String> rutas(RequestMapping rm) {
        if (rm == null || rm.path().length == 0) {
            return List.of("");
        }
        return Arrays.asList(rm.path());
    }

    private static String unir(String base, String sub) {
        String b = base == null ? "" : base;
        String s = sub == null ? "" : sub;
        if (!s.isEmpty() && !s.startsWith("/")) {
            s = "/" + s;
        }
        String ruta = b + s;
        if (ruta.isEmpty()) {
            return "/";
        }
        return ruta.length() > 1 && ruta.endsWith("/") ? ruta.substring(0, ruta.length() - 1) : ruta;
    }

    /** Refleja las reglas de SecurityConfig (documental). */
    private static String accesoRest(String ruta) {
        if (ruta.startsWith("/api/admin") || ruta.startsWith("/api/usuarios")) {
            return "Solo ADMIN (JWT)";
        }
        if (ruta.startsWith("/api/auth") || ruta.startsWith("/api/esb")
                || ruta.startsWith("/api/paquetes") || ruta.startsWith("/api/lugares")) {
            return "Público";
        }
        return "Usuario autenticado (JWT)";
    }
}