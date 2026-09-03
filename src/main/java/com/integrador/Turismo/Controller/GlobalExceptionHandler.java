package com.integrador.Turismo.Controller;
import com.integrador.Turismo.Monitoring.logging.Event;
import com.integrador.Turismo.Monitoring.logging.EventLogger;
import com.integrador.Turismo.Monitoring.logging.EventSeverity;
import com.integrador.Turismo.Monitoring.logging.EventType;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Errores de validación (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {

        logError(ex, "VALIDATION", HttpStatus.BAD_REQUEST);

        Map<String, String> errores = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(errores);
    }

    // Email duplicado, paquete no disponible, etc.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArg(IllegalArgumentException ex) {

        logError(ex, "ILLEGAL_ARGUMENT", HttpStatus.BAD_REQUEST);

        return ResponseEntity.badRequest()
                .body(Map.of("error", ex.getMessage()));
    }

    // Recurso no encontrado
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {

        logError(ex, "RUNTIME_EXCEPTION", HttpStatus.NOT_FOUND);

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    // Credenciales incorrectas en login
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {

        logError(ex, "BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Email o contraseña incorrectos"));
    }

    // Sin permisos (rol incorrecto)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {

        logError(ex, "ACCESS_DENIED", HttpStatus.FORBIDDEN);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "No tienes permiso para realizar esta acción"));
    }

    // Cancelar reserva completada, etc.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {

        logError(ex, "ILLEGAL_STATE", HttpStatus.CONFLICT);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    // Cancelar reserva ajena
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(SecurityException ex) {

        logError(ex, "SECURITY", HttpStatus.FORBIDDEN);

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage()));
    }


    private final EventLogger eventLogger;

    public GlobalExceptionHandler(EventLogger eventLogger) {
        this.eventLogger = eventLogger;
    }

    private void logError(Exception ex, String type, HttpStatus status) {

        Event event = Event.builder()
                .type(EventType.SYSTEM)
                .severity(EventSeverity.ERROR)
                .message(ex.getMessage())
                .statusCode(status.value())
                .httpMethod(type)
                .build();

        eventLogger.log(event);
    }
}
