package com.alimentared.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ConfirmarEntregaRequest(
        @NotNull(message = "La cantidad retirada es obligatoria")
        @DecimalMin(value = "0.001", message = "La cantidad retirada debe ser mayor a cero")
        @Digits(integer = 9, fraction = 3)
        BigDecimal cantidadRetirada,

        @Size(max = 500)
        String observaciones
) {
}
