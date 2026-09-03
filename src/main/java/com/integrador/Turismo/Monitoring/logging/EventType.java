package com.integrador.Turismo.Monitoring.logging;

public enum EventType {
    /**
     * Inicio de sesión, cierre de sesión,
     * validación de JWT, autenticación.
     */
    AUTHENTICATION,

    /**
     * Eventos relacionados con autorización
     * y permisos.
     */
    SECURITY,

    /**
     * Operaciones sobre la base de datos.
     */
    DATABASE,

    /**
     * Peticiones HTTP.
     */
    API,

    /**
     * Eventos internos del sistema.
     */
    SYSTEM,

    /**
     * Operaciones realizadas por usuarios.
     */
    USER,

    /**
     * Rendimiento del sistema.
     */
    PERFORMANCE,

    /**
     * Eventos de infraestructura.
     */
    INFRASTRUCTURE
}
