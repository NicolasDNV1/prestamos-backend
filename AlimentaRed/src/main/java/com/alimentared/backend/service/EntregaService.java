package com.alimentared.backend.service;

import com.alimentared.backend.dto.request.ConfirmarEntregaRequest;
import com.alimentared.backend.dto.request.NovedadEntregaRequest;
import com.alimentared.backend.dto.response.AsignacionResponse;
import com.alimentared.backend.dto.response.EntregaResponse;
import com.alimentared.backend.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EntregaService {

    /** RF07: registra un retiro total o parcial de la asignación. */
    EntregaResponse confirmarRecepcion(Long asignacionId, ConfirmarEntregaRequest request, Long usuarioId);

    /** RN08: la organización no se presentó; se repone el inventario y se genera un strike. */
    AsignacionResponse registrarNoRecogida(Long asignacionId, NovedadEntregaRequest request, Long usuarioId);

    /** RN11: la coordinación devuelve al catálogo la cantidad no retirada. */
    AsignacionResponse restockSobrante(Long asignacionId, NovedadEntregaRequest request, Long usuarioId);

    PageResponse<AsignacionResponse> listarAsignaciones(Long usuarioId, Pageable pageable);

    AsignacionResponse obtenerAsignacion(Long asignacionId, Long usuarioId);

    List<EntregaResponse> listarEntregas(Long asignacionId, Long usuarioId);
}
