//Mensajeria\RabbitConfig.java
//Mensajeria\RabbitConfig.java
package com.integrador.Turismo.Mensajeria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de RabbitMQ para los eventos de reserva (pieza 5: mensajería).
 * Un exchange fanout reparte cada evento a una cola por suscriptor.
 * Solo se activa con aqpgo.esb.broker=rabbit.
 */
@Configuration
@ConditionalOnProperty(name = "aqpgo.esb.broker", havingValue = "rabbit")
public class RabbitConfig {

    public static final String EXCHANGE = "aqpgo.reserva.eventos";
    public static final String COLA_NOTIFICACIONES = "aqpgo.reserva.notificaciones";
    public static final String COLA_AUDITORIA = "aqpgo.reserva.auditoria";
    public static final String COLA_PROVEEDORES = "aqpgo.reserva.proveedores";

    @Bean
    FanoutExchange reservaEventos() {
        return new FanoutExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue colaNotificaciones() {
        return QueueBuilder.durable(COLA_NOTIFICACIONES).build();
    }

    @Bean
    Queue colaAuditoria() {
        return QueueBuilder.durable(COLA_AUDITORIA).build();
    }

    @Bean
    Queue colaProveedores() {
        return QueueBuilder.durable(COLA_PROVEEDORES).build();
    }

    @Bean
    Binding bindingNotificaciones(FanoutExchange reservaEventos,
            @Qualifier("colaNotificaciones") Queue colaNotificaciones) {
        return BindingBuilder.bind(colaNotificaciones).to(reservaEventos);
    }

    @Bean
    Binding bindingAuditoria(FanoutExchange reservaEventos,
            @Qualifier("colaAuditoria") Queue colaAuditoria) {
        return BindingBuilder.bind(colaAuditoria).to(reservaEventos);
    }

    @Bean
    Binding bindingProveedores(FanoutExchange reservaEventos,
            @Qualifier("colaProveedores") Queue colaProveedores) {
        return BindingBuilder.bind(colaProveedores).to(reservaEventos);
    }
}