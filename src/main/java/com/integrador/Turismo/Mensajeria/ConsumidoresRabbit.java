//Mensajeria\ConsumidoresRabbit.java
package com.integrador.Turismo.Mensajeria;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Un consumidor por suscriptor, cada uno sobre su propia cola de RabbitMQ.
 * Corren en hilos distintos: no se esperan entre sí (coreografía).
 */
@Component
@ConditionalOnProperty(name = "aqpgo.esb.broker", havingValue = "rabbit")
@RequiredArgsConstructor
public class ConsumidoresRabbit {

    private final ColaEsb cola;

    @RabbitListener(queues = RabbitConfig.COLA_NOTIFICACIONES)
    public void notificaciones(String cuerpo) {
        cola.recibirDelBroker("Notificaciones", cuerpo);
    }

    @RabbitListener(queues = RabbitConfig.COLA_AUDITORIA)
    public void auditoria(String cuerpo) {
        cola.recibirDelBroker("Auditoría", cuerpo);
    }
}