package com.alimentared.backend.service;

import com.alimentared.backend.dto.response.HistorialEstadoResponse;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.EntidadAuditada;

import java.util.List;

/** RN04: registro de cada cambio de estado con fecha exacta y usuario responsable. */
public interface AuditoriaService {

    void registrar(EntidadAuditada entidad, Long entidadId, Object estadoAnterior, Object estadoNuevo,
                   Usuario responsable, String justificacion);

    List<HistorialEstadoResponse> listar(EntidadAuditada entidad, Long entidadId);
}
