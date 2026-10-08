package com.integrador.Turismo.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Registro de servicios del bus: catálogo de contratos (WSDL) y operaciones
 * SOAP publicadas, construido en tiempo de ejecución a partir de los
 * endpoints realmente desplegados.
 */
@RestController
@RequestMapping("/api/esb")
@RequiredArgsConstructor
public class EsbRegistroController {

    private final ApplicationContext context;

    @GetMapping("/registro")
    public Map<String, Object> registro() {

        List<Map<String, String>> operaciones = new ArrayList<>();

        for (Object bean : context.getBeansWithAnnotation(Endpoint.class).values()) {
            Class<?> clazz = AopUtils.getTargetClass(bean);
            for (Method m : clazz.getDeclaredMethods()) {
                PayloadRoot root = AnnotatedElementUtils.findMergedAnnotation(m, PayloadRoot.class);
                if (root != null) {
                    operaciones.add(Map.of(
                            "servicio", clazz.getSimpleName(),
                            "operacion", root.localPart(),
                            "namespace", root.namespace(),
                            "metodo", m.getName()));
                }
            }
        }

        operaciones.sort(Comparator.comparing(
                (Map<String, String> o) -> o.get("namespace") + o.get("operacion")));

        List<Map<String, String>> contratos = context.getBeansOfType(DefaultWsdl11Definition.class)
                .keySet().stream()
                .sorted()
                .map(nombre -> Map.of("nombre", nombre, "wsdl", "/ws/" + nombre + ".wsdl"))
                .toList();

        return Map.of("contratos", contratos, "operaciones", operaciones);
    }
}