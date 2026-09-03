package com.integrador.Turismo.Monitoring.logging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Representa un evento generado por el sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private String eventId;
    /**
     * Fecha y hora del evento.
     */
    private LocalDateTime timestamp;

    /**
     * Tipo del evento.
     */
    private EventType type;

    /**
     * Nivel de severidad.
     */
    private EventSeverity severity;

    /**
     * Descripción del evento.
     */
    private String message;

    /**
     * Endpoint donde ocurrió el evento.
     */
    private String endpoint;

    /**
     * Usuario autenticado.
     */
    private String username;

    /**
     * Dirección IP del cliente.
     */
    private String ipAddress;

    /**
     * Método HTTP.
     */
    private String httpMethod;

    /**
     * Código de respuesta HTTP.
     */
    private Integer statusCode;

    /**
     * Tiempo de ejecución en milisegundos.
     */
    private Long executionTime;

    /**
     * Excepción asociada al evento.
     */
    private String exception;
}
