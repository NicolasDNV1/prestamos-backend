package com.alimentared.backend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Parámetros generales del sistema (configurables por variables de entorno). */
@Validated
@ConfigurationProperties(prefix = "alimentared")
public record AlimentaRedProperties(
        @NotBlank String zonaHoraria,
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Reglas reglas,
        @Valid @NotNull Seed seed
) {

    public record Jwt(
            @NotBlank String secret,
            @Positive long expiracionMinutos
    ) {
    }

    public record Reglas(
            /* RN07 */ @Positive int maxSolicitudesActivas,
            /* RN08 */ @Positive int maxStrikes,
            /* RN06 */ @Positive long slaConfirmacionMinutos,
            /* RN09 */ @Positive long ventanaEmpateSegundos,
            /* RF07 */ @Positive long horasLimiteRetiro,
            /* RF03 */ @Positive long horasRescateUrgente
    ) {
    }

    public record Seed(
            boolean habilitado,
            String adminCorreo,
            String adminContrasena,
            String coordinadorCorreo,
            String coordinadorContrasena
    ) {
    }
}
