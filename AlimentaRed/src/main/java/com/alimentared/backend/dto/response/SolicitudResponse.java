package com.alimentared.backend.dto.response;

import com.alimentared.backend.entity.enums.EstadoSolicitud;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record SolicitudResponse(
        Long id,
        Long loteId,
        String loteNombre,
        String unidadMedida,
        Long organizacionId,
        String organizacionNombre,
        BigDecimal cantidadSolicitada,
        EstadoSolicitud estado,
        boolean enEmpate,
        LocalDateTime fechaCreacion,
        /* RN06: momento en que la solicitud expira si sigue pendiente. */
        LocalDateTime fechaLimiteConfirmacion,
        LocalDateTime fechaResolucion,
        String motivoResolucion
) {
}
