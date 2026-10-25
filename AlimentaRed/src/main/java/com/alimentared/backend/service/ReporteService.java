package com.alimentared.backend.service;

import com.alimentared.backend.dto.response.EntregaResponse;
import com.alimentared.backend.dto.response.PageResponse;
import com.alimentared.backend.dto.response.ReporteImpactoResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ReporteService {

    ReporteImpactoResponse consultarImpacto(LocalDate desde, LocalDate hasta, Long categoriaId);

    PageResponse<EntregaResponse> consultarHistorial(LocalDate desde, LocalDate hasta, Long categoriaId,
                                                     Long usuarioId, Pageable pageable);
}
