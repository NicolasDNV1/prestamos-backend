package com.alimentared.backend.dto.response;

import com.alimentared.backend.entity.enums.EstadoLote;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record IncidenciaResponse(
        Long id,
        Long loteId,
        String loteNombre,
        String detalles,
        EstadoLote estadoLoteAnterior,
        String reportadoPor,
        LocalDateTime fechaReporte,
        boolean resuelta,
        String resolucion,
        String resueltoPor,
        LocalDateTime fechaResolucion
) {
}
