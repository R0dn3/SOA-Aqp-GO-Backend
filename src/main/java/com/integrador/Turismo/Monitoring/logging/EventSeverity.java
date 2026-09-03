package com.integrador.Turismo.Monitoring.logging;

public enum EventSeverity {
    /**
     * Evento informativo.
     * No requiere ninguna acción.
     */
    INFO,

    /**
     * Evento que podría convertirse en un problema.
     */
    WARNING,

    /**
     * Error que afecta una funcionalidad,
     * pero no compromete completamente el sistema.
     */
    ERROR,

    /**
     * Error crítico que requiere atención inmediata.
     */
    CRITICAL
}
