package com.integrador.Turismo.Monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {

    /**
     * Número total de eventos registrados.
     */
    @Bean
    public Counter applicationEventsCounter(MeterRegistry registry) {

        return Counter.builder("aqpgo.events.total")
                .description("Número total de eventos del sistema")
                .register(registry);

    }

    /**
     * Número total de errores.
     */
    @Bean
    public Counter applicationErrorsCounter(MeterRegistry registry) {

        return Counter.builder("aqpgo.errors.total")
                .description("Número total de errores")
                .register(registry);

    }

    /**
     * Número total de eventos críticos.
     */
    @Bean
    public Counter criticalEventsCounter(MeterRegistry registry) {

        return Counter.builder("aqpgo.events.critical")
                .description("Eventos críticos")
                .register(registry);

    }

    /**
     * Tiempo de respuesta de operaciones internas.
     */
    @Bean
    public Timer applicationTimer(MeterRegistry registry) {

        return Timer.builder("aqpgo.request.execution")
                .description("Tiempo de ejecución de procesos")
                .publishPercentiles(0.50, 0.90, 0.95, 0.99)
                .register(registry);

    }
}
