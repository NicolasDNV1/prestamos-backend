package com.alimentared.backend.dto.response;

import com.alimentared.backend.entity.enums.EstadoAlimento;
import com.alimentared.backend.entity.enums.EstadoLote;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record LoteResponse(
        Long id,
        String nombre,
        String descripcion,
        Long categoriaId,
        String categoriaNombre,
        String unidadMedida,
        Long donanteId,
        String donanteNombre,
        BigDecimal cantidadInicial,
        BigDecimal cantidadDisponible,
        BigDecimal unidadMinima,
        BigDecimal multiplo,
        EstadoLote estado,
        EstadoAlimento estadoAlimento,
        String ubicacion,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaCaducidad,
        /* RF03: caduca en menos de 4 horas. */
        boolean rescateUrgente,
        /* RF04/RF06: solo visibles para el donante, coordinación o la receptora asignada. */
        @JsonInclude(JsonInclude.Include.NON_NULL) String instruccionesRecogida,
        @JsonInclude(JsonInclude.Include.NON_NULL) String contacto
) {
}
