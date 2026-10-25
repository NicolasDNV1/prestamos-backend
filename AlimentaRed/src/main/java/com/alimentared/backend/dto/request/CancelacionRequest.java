package com.alimentared.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelacionRequest(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 5, max = 500)
        String motivo
) {
}
