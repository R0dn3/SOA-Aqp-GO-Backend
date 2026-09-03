package com.integrador.Turismo.Monitoring.filter;


import com.integrador.Turismo.Monitoring.logging.EventSeverity;
import com.integrador.Turismo.Monitoring.logging.EventType;
import com.integrador.Turismo.Monitoring.logging.EventLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.integrador.Turismo.Monitoring.logging.Event;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
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
                    .type(EventType.API)
                    .severity(severity)
                    .message(request.getMethod() + " " + request.getRequestURI())
                    .endpoint(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .statusCode(response.getStatus())
                    .executionTime(executionTime)
                    .username(
                            request.getUserPrincipal() != null ?
                                    request.getUserPrincipal().getName() :
                                    "ANONYMOUS"
                    )
                    .ipAddress(request.getRemoteAddr())
                    .build();

            eventLogger.log(event);

        }

    }
}
