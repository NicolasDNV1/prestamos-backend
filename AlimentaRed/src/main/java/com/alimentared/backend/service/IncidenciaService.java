package com.alimentared.backend.service;

import com.alimentared.backend.dto.request.IncidenciaRequest;
import com.alimentared.backend.dto.request.ResolverIncidenciaRequest;
import com.alimentared.backend.dto.response.IncidenciaResponse;
import com.alimentared.backend.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IncidenciaService {

    IncidenciaResponse reportarIncidencia(IncidenciaRequest request, Long usuarioId);

    IncidenciaResponse resolverIncidencia(Long incidenciaId, ResolverIncidenciaRequest request, Long usuarioId);

    PageResponse<IncidenciaResponse> listarIncidencias(Boolean resuelta, Pageable pageable);

    List<IncidenciaResponse> listarPorLote(Long loteId, Long usuarioId);
}
