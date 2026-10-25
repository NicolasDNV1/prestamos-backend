package com.alimentared.backend.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record StrikeResponse(
        Long id,
        Long organizacionId,
        String organizacionNombre,
        Long asignacionId,
        String nombreStrike,
        String detalles,
        LocalDateTime fechaRegistro,
        boolean vigente,
        String justificacionApelacion,
        LocalDateTime fechaApelacion
) {
}
