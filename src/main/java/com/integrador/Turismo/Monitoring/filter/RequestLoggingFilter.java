package com.integrador.Turismo.Monitoring.filter;

import com.integrador.Turismo.Monitoring.logging.Event;
import com.integrador.Turismo.Monitoring.logging.EventLogger;
import com.integrador.Turismo.Monitoring.logging.EventSeverity;
import com.integrador.Turismo.Monitoring.logging.EventType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_KEY = "correlationId";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final EventLogger eventLogger;

    public RequestLoggingFilter(EventLogger eventLogger) {
        this.eventLogger = eventLogger;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();

        // Si el cliente (p. ej. el BFF) ya mandó un ID válido, se respeta;
        // si no, se genera uno. Se valida el formato para evitar inyección en logs.
        String incoming = request.getHeader(CORRELATION_HEADER);
        String correlationId = (incoming != null && incoming.matches("^[A-Za-z0-9-]{8,64}$"))
                ? incoming
                : UUID.randomUUID().toString();

        MDC.put(CORRELATION_KEY, correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);

        try {

            filterChain.doFilter(request, response);

        } finally {

            long executionTime = System.currentTimeMillis() - start;

            EventSeverity severity;

            if (response.getStatus() >= 500)
                severity = EventSeverity.CRITICAL;

            else if (response.getStatus() >= 400)
                severity = EventSeverity.WARNING;

            else
                severity = EventSeverity.INFO;

            Event event = Event.builder()
                    .eventId(correlationId)
                    .type(EventType.API)
                    .severity(severity)
                    .message(request.getMethod() + " " + request.getRequestURI())
                    .endpoint(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .statusCode(response.getStatus())
                    .executionTime(executionTime)
                    .username(
                            request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "ANONYMOUS")
                    .ipAddress(request.getRemoteAddr())
                    .build();

            eventLogger.log(event);

            MDC.remove(CORRELATION_KEY);

        }

    }
}