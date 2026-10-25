package com.alimentared.backend.dto.request;

import com.alimentared.backend.entity.enums.EstadoAlimento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoteRequest(
        @NotBlank(message = "El nombre del alimento es obligatorio")
        @Size(max = 120)
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 10, max = 500, message = "La descripción debe tener entre 10 y 500 caracteres")
        String descripcion,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a cero")
        @Digits(integer = 9, fraction = 3)
        BigDecimal cantidad,

        @NotNull(message = "La unidad mínima es obligatoria")
        @DecimalMin(value = "0.001", message = "La unidad mínima debe ser mayor a cero")
        @Digits(integer = 9, fraction = 3)
        BigDecimal unidadMinima,

        @NotNull(message = "El múltiplo es obligatorio")
        @DecimalMin(value = "0.001", message = "El múltiplo debe ser mayor a cero")
        @Digits(integer = 9, fraction = 3)
        BigDecimal multiplo,

        @NotNull(message = "El estado del alimento es obligatorio")
        EstadoAlimento estadoAlimento,

        @NotNull(message = "La fecha límite de consumo es obligatoria")
        @Future(message = "La fecha límite de consumo debe estar en el futuro")
        LocalDateTime fechaCaducidad,

        @Size(max = 200)
        String ubicacion,

        @Size(max = 500)
        String instruccionesRecogida,

        @Size(max = 150)
        String contacto
) {
}
