package com.alimentared.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolverIncidenciaRequest(
        @NotNull(message = "Indique si el lote se reactiva o se cancela")
        Boolean reactivarLote,

        @NotBlank(message = "La resolución es obligatoria")
        @Size(min = 5, max = 500)
        String resolucion
) {
}
