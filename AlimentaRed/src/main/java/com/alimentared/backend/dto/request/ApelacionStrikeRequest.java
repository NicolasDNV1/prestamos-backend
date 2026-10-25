package com.alimentared.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** RN12: evaluación de la apelación de un strike por parte del Coordinador. */
public record ApelacionStrikeRequest(
        @NotNull(message = "El strike es obligatorio")
        Long strikeId,

        @NotBlank(message = "La justificación es obligatoria")
        @Size(min = 10, max = 500)
        String justificacion,

        @NotNull(message = "Indique si la apelación se aprueba")
        Boolean aprobada
) {
}
