package com.integrador.Turismo.Monitoring.logging;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class EventLogger {


    private static final Logger logger =
            LoggerFactory.getLogger(EventLogger.class);

    /**
     * Punto único de entrada para registrar eventos.
     */
    public void log(Event event) {

        enrich(event);

        String logMessage = String.format(
                "[%s] [%s] [%s] %s | %s %s | HTTP %d | %d ms",
                event.getEventId(),
                event.getSeverity(),
                event.getType(),
                event.getMessage(),
                event.getHttpMethod(),
                event.getEndpoint(),
                event.getStatusCode(),
                event.getExecutionTime()
        );

        switch (event.getSeverity()) {

            case INFO -> logger.info(logMessage);

            case WARNING -> logger.warn(logMessage);

            case ERROR -> logger.error(logMessage);

            case CRITICAL -> logger.error("🚨 {}", logMessage);

        }
        if (event.getSeverity() == EventSeverity.ERROR
                || event.getSeverity() == EventSeverity.CRITICAL) {

            Sentry.withScope(scope -> {

                scope.setLevel(
                        event.getSeverity() == EventSeverity.CRITICAL ?
                                SentryLevel.FATAL :
                                SentryLevel.ERROR
                );

                scope.setTag("eventId", event.getEventId());

                scope.setTag("type", event.getType().name());

                scope.setTag("severity", event.getSeverity().name());

                if (event.getEndpoint() != null)
                    scope.setTag("endpoint", event.getEndpoint());

                if (event.getHttpMethod() != null)
                    scope.setTag("method", event.getHttpMethod());

                if (event.getStatusCode() != null)
                    scope.setTag("status",
                            String.valueOf(event.getStatusCode()));

                if (event.getExecutionTime() != null)
                    scope.setExtra(
                            "executionTime",
                            String.valueOf(event.getExecutionTime())
                    );

                if (event.getUsername() != null)
                    scope.setTag("username", event.getUsername());

                Sentry.captureMessage(event.getMessage());

            });

        }

        /*
         * Próximas fases:
         *
         * Sentry
         * Better Stack
         * Jira
         */

    }

    /**
     * Completa automáticamente la información del evento.
     */
    private void enrich(Event event) {

        if (event.getEventId() == null)
            event.setEventId(UUID.randomUUID().toString());

        if (event.getTimestamp() == null)
            event.setTimestamp(LocalDateTime.now());

    }

}
