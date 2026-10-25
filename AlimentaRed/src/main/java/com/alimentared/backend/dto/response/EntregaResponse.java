package com.alimentared.backend.dto.response;

import com.alimentared.backend.entity.enums.EstadoEntrega;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record EntregaResponse(
        Long id,
        Long asignacionId,
        Long solicitudId,
        Long loteId,
        String loteNombre,
        String categoriaNombre,
        String unidadMedida,
        Long donanteId,
        String donanteNombre,
        Long receptoraId,
        String receptoraNombre,
        BigDecimal cantidadRetirada,
        EstadoEntrega estado,
        LocalDateTime fechaEntrega,
        String observaciones,
        String responsable,
        boolean fueraDeFranja
) {
}
