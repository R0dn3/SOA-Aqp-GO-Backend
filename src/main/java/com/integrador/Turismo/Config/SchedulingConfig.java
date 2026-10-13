package com.integrador.Turismo.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita @Scheduled (reproceso automático de mensajes del ESB). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}