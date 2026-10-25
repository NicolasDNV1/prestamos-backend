package com.alimentared.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoAlimentoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        String nombre,

        @Size(max = 300)
        String descripcion,

        @NotBlank(message = "La unidad de medida es obligatoria")
        @Size(max = 20)
        String unidadMedida
) {
}
