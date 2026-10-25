package com.alimentared.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas programadas (RN06 SLA, RN09 empate, vencimiento de lotes). */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "alimentared.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
