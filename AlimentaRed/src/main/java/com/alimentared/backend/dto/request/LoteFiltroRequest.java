package com.alimentared.backend.dto.request;

import com.alimentared.backend.entity.enums.EstadoLote;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** RF04: filtros combinables del catálogo de lotes (rango sobre la fecha de caducidad). */
public record LoteFiltroRequest(
        Long categoriaId,
        String ubicacion,
        EstadoLote estado,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta
) {
}
