package com.integrador.Turismo.Monitoring.health;


import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        long uptime = runtime.getUptime();

        return Health.up()
                .withDetail("application", "AqpGo Backend")
                .withDetail("status", "Running")
                .withDetail("uptimeMilliseconds", uptime)
                .withDetail("uptimeSeconds", uptime / 1000)
                .withDetail("javaVersion", System.getProperty("java.version"))
                .build();

    }
}
