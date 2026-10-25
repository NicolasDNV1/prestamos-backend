package com.alimentared.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(AlimentaRedProperties.class)
public class AppConfig {

    /** Reloj único del sistema: permite fijar el tiempo en pruebas de SLA y caducidad. */
    @Bean
    public Clock clock(AlimentaRedProperties properties) {
        return Clock.system(ZoneId.of(properties.zonaHoraria()));
    }
}
