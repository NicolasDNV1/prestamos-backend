package com.alimentared.backend.dto.response;

import com.alimentared.backend.entity.enums.EstadoAsignacion;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record AsignacionResponse(
        Long id,
        Long solicitudId,
        Long loteId,
        String loteNombre,
        String unidadMedida,
        Long organizacionId,
        String organizacionNombre,
        Long donanteId,
        String donanteNombre,
        BigDecimal cantidadAsignada,
        BigDecimal cantidadRetirada,
        BigDecimal cantidadPendiente,
        EstadoAsignacion estado,
        boolean autoAsignada,
        LocalDateTime fechaAsignacion,
        LocalDateTime fechaLimiteRetiro,
        LocalDateTime fechaCierre,
        String ubicacion,
        String instruccionesRecogida,
        String contacto
) {
}
