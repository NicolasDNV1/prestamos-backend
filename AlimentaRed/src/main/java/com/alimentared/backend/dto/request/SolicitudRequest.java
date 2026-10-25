package com.alimentared.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SolicitudRequest(
        @NotNull(message = "El lote es obligatorio")
        Long loteId,

        @NotNull(message = "La cantidad solicitada es obligatoria")
        @DecimalMin(value = "0.001", message = "La cantidad solicitada debe ser mayor a cero")
        @Digits(integer = 9, fraction = 3)
        BigDecimal cantidadSolicitada
) {
}
