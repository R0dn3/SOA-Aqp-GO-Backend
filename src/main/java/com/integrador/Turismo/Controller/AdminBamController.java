package com.integrador.Turismo.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.integrador.Turismo.Model.MensajeRechazado;
import com.integrador.Turismo.Model.ProcesoInstancia;
import com.integrador.Turismo.Model.ProcesoPaso;
import com.integrador.Turismo.Repository.MensajeRechazadoRepository;
import com.integrador.Turismo.Repository.ProcesoInstanciaRepository;
import com.integrador.Turismo.Repository.ProcesoPasoRepository;

import lombok.RequiredArgsConstructor;

/**
 * BAM (Business Activity Monitoring): indicadores del negocio calculados en
 * tiempo real a partir de las trazas del proceso reservarYPagar, más alarmas
 * por reglas. Solo ADMIN (SecurityConfig protege /api/admin/**).
 *
 * Umbrales configurables (opcionales, tienen valor por defecto):
 * aqpgo.bam.umbral-duracion-ms (2000)
 * aqpgo.bam.umbral-tasa (30.0) -> % de compensación o rechazo
 * aqpgo.bam.minimo-muestras (5) -> mínimo de instancias para evaluar tasas
 */
@RestController
@RequestMapping("/api/admin/bam")
@RequiredArgsConstructor
public class AdminBamController {

    private static final List<String> PASOS = List.of(
            "ESB-VALIDATE", "ESB-IDEMPOTENCIA", "ESB-SPLIT", "ESB-ENRICH",
            "ESB-OPERATE", "ESB-ROUTE", "ESB-PUBLISH");

    private final ProcesoInstanciaRepository instanciaRepository;
    private final ProcesoPasoRepository pasoRepository;
    private final MensajeRechazadoRepository rechazadoRepository;

    @Value("${aqpgo.bam.umbral-duracion-ms:2000}")
    private long umbralDuracionMs;

    @Value("${aqpgo.bam.umbral-tasa:30.0}")
    private double umbralTasa;

    @Value("${aqpgo.bam.minimo-muestras:5}")
    private int minimoMuestras;

    public record PasoStatDto(String paso, long veces, long promedioMs, long maxMs,
            long errores, double porcentajeTiempo) {
    }

    public record PuntoDto(LocalDateTime inicio, long duracionMs, String estado) {
    }

    public record AlarmaDto(String nivel, String codigo, String mensaje) {
    }

    public record BamDto(long total, long completados, long compensados, long rechazados,
            long fallidos, long repetidos, double tasaExito, double tasaCompensacion,
            double tasaRechazo, long duracionPromedioMs, long duracionP95Ms, long ultimaHora,
            long deadLetterPendientes, String cuelloBotella, long umbralDuracionMs,
            List<PasoStatDto> pasos, List<PuntoDto> serie, List<AlarmaDto> alarmas) {
    }

    @GetMapping("/kpis")
    public BamDto kpis() {
        List<ProcesoInstancia> inst = instanciaRepository.findTop200ByOrderByInicioDesc();
        long total = inst.size();

        long completados = contar(inst, "COMPLETADO");
        long compensados = contar(inst, "COMPENSADO");
        long rechazados = contar(inst, "RECHAZADO");
        long fallidos = contar(inst, "FALLIDO");
        long repetidos = contar(inst, "REPETIDO");

        double tasaExito = pct(completados, total);
        double tasaCompensacion = pct(compensados, total);
        double tasaRechazo = pct(rechazados + fallidos, total);

        // ── Duraciones ──────────────────────────────────────────────
        List<Long> duraciones = inst.stream().map(ProcesoInstancia::getDuracionMs).sorted().toList();
        long promedio = total == 0 ? 0
                : Math.round(duraciones.stream().mapToLong(Long::longValue).average().orElse(0));
        long p95 = total == 0 ? 0 : duraciones.get((int) Math.ceil(0.95 * total) - 1);
        long ultimaHora = inst.stream()
                .filter(i -> i.getInicio().isAfter(LocalDateTime.now().minusHours(1)))
                .count();
        long promedioReciente = inst.isEmpty() ? 0
                : Math.round(inst.stream().limit(10).mapToLong(ProcesoInstancia::getDuracionMs)
                        .average().orElse(0));

        // ── Tiempo por paso (cuello de botella) ─────────────────────
        List<String> ids = inst.stream().map(ProcesoInstancia::getId).toList();
        Map<String, List<ProcesoPaso>> porPaso = ids.isEmpty()
                ? Map.of()
                : pasoRepository.findByInstanciaIdInOrderByOrdenAsc(ids).stream()
                        .filter(p -> PASOS.contains(p.getPaso()))
                        .collect(Collectors.groupingBy(ProcesoPaso::getPaso));

        Map<String, Long> promedios = new LinkedHashMap<>();
        for (String nombre : PASOS) {
            List<ProcesoPaso> lista = porPaso.get(nombre);
            if (lista == null || lista.isEmpty()) {
                continue;
            }
            promedios.put(nombre, Math.round(lista.stream()
                    .mapToLong(ProcesoPaso::getDuracionMs).average().orElse(0)));
        }
        double sumaPromedios = promedios.values().stream().mapToLong(Long::longValue).sum();

        List<PasoStatDto> pasos = new ArrayList<>();
        String cuello = null;
        long mayor = -1;
        for (Map.Entry<String, Long> e : promedios.entrySet()) {
            List<ProcesoPaso> lista = porPaso.get(e.getKey());
            long max = lista.stream().mapToLong(ProcesoPaso::getDuracionMs).max().orElse(0);
            long errores = lista.stream().filter(p -> "ERROR".equals(p.getEstado())).count();
            double porcentaje = sumaPromedios == 0 ? 0
                    : Math.round(e.getValue() * 1000.0 / sumaPromedios) / 10.0;
            pasos.add(new PasoStatDto(e.getKey(), lista.size(), e.getValue(), max, errores, porcentaje));
            if (e.getValue() > mayor) {
                mayor = e.getValue();
                cuello = e.getKey();
            }
        }

        // ── Serie para la tendencia (últimas 20, de la más antigua a la más nueva) ──
        List<PuntoDto> serie = new ArrayList<>(inst.stream().limit(20)
                .map(i -> new PuntoDto(i.getInicio(), i.getDuracionMs(), i.getEstado()))
                .toList());
        Collections.reverse(serie);

        // ── Alarmas ─────────────────────────────────────────────────
        List<MensajeRechazado> pendientes = rechazadoRepository.findPendientesReproceso();
        long brokerCaido = pendientes.stream().filter(m -> "TODOS".equals(m.getSuscriptor())).count();
        Map<String, Long> porSuscriptor = pendientes.stream()
                .filter(m -> !"TODOS".equals(m.getSuscriptor()))
                .collect(Collectors.groupingBy(MensajeRechazado::getSuscriptor, TreeMap::new,
                        Collectors.counting()));

        List<AlarmaDto> alarmas = new ArrayList<>();
        if (brokerCaido > 0) {
            alarmas.add(new AlarmaDto("CRITICA", "BROKER_CAIDO",
                    "RabbitMQ no disponible: " + brokerCaido
                            + " evento(s) sin publicar, en dead letter esperando reproceso"));
        }
        if (!porSuscriptor.isEmpty()) {
            String detalle = porSuscriptor.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(", "));
            alarmas.add(new AlarmaDto("ADVERTENCIA", "ENTREGAS_FALLIDAS",
                    "Entregas fallidas pendientes de reproceso (" + detalle + ")"));
        }
        if (total >= minimoMuestras && tasaCompensacion > umbralTasa) {
            alarmas.add(new AlarmaDto("ADVERTENCIA", "COMPENSACION_ALTA",
                    "Compensaciones en " + tasaCompensacion + "% de los procesos (umbral "
                            + umbralTasa + "%)"));
        }
        if (total >= minimoMuestras && tasaRechazo > umbralTasa) {
            alarmas.add(new AlarmaDto("ADVERTENCIA", "RECHAZO_ALTO",
                    "Rechazos o fallos en " + tasaRechazo + "% de los procesos (umbral "
                            + umbralTasa + "%)"));
        }
        if (!inst.isEmpty() && promedioReciente > umbralDuracionMs) {
            alarmas.add(new AlarmaDto("ADVERTENCIA", "PROCESO_LENTO",
                    "Duración promedio de los últimos 10 procesos: " + promedioReciente
                            + " ms (umbral " + umbralDuracionMs + " ms)"));
        }
        if (cuello != null && sumaPromedios > 0) {
            final String paso = cuello;
            double porcentaje = pasos.stream().filter(p -> p.paso().equals(paso))
                    .findFirst().map(PasoStatDto::porcentajeTiempo).orElse(0.0);
            alarmas.add(new AlarmaDto("INFO", "CUELLO_DE_BOTELLA",
                    "Cuello de botella: " + cuello + " (" + promedios.get(cuello) + " ms de promedio, "
                            + porcentaje + "% del tiempo del proceso)"));
        }

        return new BamDto(total, completados, compensados, rechazados, fallidos, repetidos,
                tasaExito, tasaCompensacion, tasaRechazo, promedio, p95, ultimaHora,
                pendientes.size(), cuello, umbralDuracionMs, pasos, serie, alarmas);
    }

    private static long contar(List<ProcesoInstancia> inst, String estado) {
        return inst.stream().filter(i -> estado.equals(i.getEstado())).count();
    }

    private static double pct(long parte, long total) {
        return total == 0 ? 0 : Math.round(parte * 1000.0 / total) / 10.0;
    }
}