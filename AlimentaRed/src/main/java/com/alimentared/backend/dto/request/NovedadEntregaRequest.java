package com.alimentared.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Novedad logística: no recogida (RN08) o restock del sobrante (RN11). */
public record NovedadEntregaRequest(
        @NotBlank(message = "Las observaciones son obligatorias")
        @Size(min = 5, max = 500)
        String observaciones
) {
}
