package com.integrador.Turismo.Monitoring.actuator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class CustomInfoContributor implements InfoContributor {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${info.app.version}")
    private String version;

    @Value("${info.app.description}")
    private String description;

    @Override
    public void contribute(Info.Builder builder) {

        builder.withDetail("application", applicationName)
                .withDetail("version", version)
                .withDetail("description", description)
                .withDetail("javaVersion", System.getProperty("java.version"))
                .withDetail("operatingSystem", System.getProperty("os.name"))
                .withDetail("environment", "Render Free")
                .withDetail("database", "Supabase PostgreSQL")
                .withDetail("frontend", "Next.js - Vercel");

    }
}
