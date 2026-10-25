package com.alimentared.backend.service;

import com.alimentared.backend.dto.request.CancelacionRequest;
import com.alimentared.backend.dto.request.LoteFiltroRequest;
import com.alimentared.backend.dto.request.LoteRequest;
import com.alimentared.backend.dto.response.HistorialEstadoResponse;
import com.alimentared.backend.dto.response.LoteResponse;
import com.alimentared.backend.dto.response.PageResponse;
import com.alimentared.backend.entity.enums.EstadoLote;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LoteService {

    LoteResponse publicarLote(LoteRequest request, Long usuarioId);

    LoteResponse actualizarLote(Long loteId, LoteRequest request, Long usuarioId);

    LoteResponse publicarBorrador(Long loteId, Long usuarioId);

    LoteResponse cancelarLote(Long loteId, CancelacionRequest request, Long usuarioId);

    LoteResponse obtenerLote(Long loteId, Long usuarioId);

    PageResponse<LoteResponse> filtrarLotes(LoteFiltroRequest filtro, Pageable pageable, Long usuarioId);

    PageResponse<LoteResponse> listarMisLotes(EstadoLote estado, Pageable pageable, Long usuarioId);

    List<HistorialEstadoResponse> obtenerHistorial(Long loteId, Long usuarioId);

    /** Tarea programada: marca como vencidos los lotes que superaron su fecha límite (RN01). */
    void marcarLotesVencidos();
}
