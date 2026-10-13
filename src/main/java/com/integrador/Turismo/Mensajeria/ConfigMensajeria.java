package com.integrador.Turismo.Mensajeria;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Interruptor en memoria para la demostración: permite simular que el servicio
 * de notificaciones falla, sin reiniciar el backend. Su valor inicial sale de
 * aqpgo.esb.cola.simular-fallo.
 */
@Component
public class ConfigMensajeria {

    private final AtomicBoolean simularFalloNotificaciones;

    public ConfigMensajeria(@Value("${aqpgo.esb.cola.simular-fallo:false}") boolean inicial) {
        this.simularFalloNotificaciones = new AtomicBoolean(inicial);
    }

    public boolean isSimularFalloNotificaciones() {
        return simularFalloNotificaciones.get();
    }

    public void setSimularFalloNotificaciones(boolean valor) {
        simularFalloNotificaciones.set(valor);
    }
}