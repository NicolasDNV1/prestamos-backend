package com.alimentared.backend.dto.response;

import com.alimentared.backend.entity.enums.EntidadAuditada;

import java.time.LocalDateTime;

public record HistorialEstadoResponse(
        Long id,
        EntidadAuditada entidad,
        Long entidadId,
        String estadoAnterior,
        String estadoNuevo,
        String responsable,
        LocalDateTime fecha,
        String justificacion
) {
}
