package com.alimentared.backend.mapper;

import com.alimentared.backend.dto.response.HistorialEstadoResponse;
import com.alimentared.backend.entity.HistorialEstado;
import org.springframework.stereotype.Component;

@Component
public class HistorialEstadoMapper {

    private static final String SISTEMA = "SISTEMA";

    public HistorialEstadoResponse toResponse(HistorialEstado historial) {
        return new HistorialEstadoResponse(
                historial.getIdHistorial(),
                historial.getEntidad(),
                historial.getEntidadId(),
                historial.getEstadoAnterior(),
                historial.getEstadoNuevo(),
                historial.getUsuarioResponsable() != null ? historial.getUsuarioResponsable().getNombre() : SISTEMA,
                historial.getFecha(),
                historial.getJustificacion());
    }
}
