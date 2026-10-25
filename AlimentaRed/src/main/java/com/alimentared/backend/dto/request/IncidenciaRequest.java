package com.alimentared.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncidenciaRequest(
        @NotNull(message = "El lote es obligatorio")
        Long loteId,

        @NotBlank(message = "La justificación de la incidencia es obligatoria")
        @Size(min = 10, max = 500, message = "Los detalles deben tener entre 10 y 500 caracteres")
        String detalles
) {
}
