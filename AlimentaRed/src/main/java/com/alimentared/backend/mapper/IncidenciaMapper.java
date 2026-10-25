package com.alimentared.backend.mapper;

import com.alimentared.backend.dto.response.IncidenciaResponse;
import com.alimentared.backend.entity.Incidencia;
import org.springframework.stereotype.Component;

@Component
public class IncidenciaMapper {

    public IncidenciaResponse toResponse(Incidencia incidencia) {
        return IncidenciaResponse.builder()
                .id(incidencia.getIdIncidencia())
                .loteId(incidencia.getLote().getIdLote())
                .loteNombre(incidencia.getLote().getNombre())
                .detalles(incidencia.getDetalles())
                .estadoLoteAnterior(incidencia.getEstadoLoteAnterior())
                .reportadoPor(incidencia.getUsuarioReporta().getNombre())
                .fechaReporte(incidencia.getFechaReporte())
                .resuelta(incidencia.isResuelta())
                .resolucion(incidencia.getResolucion())
                .resueltoPor(incidencia.getUsuarioResuelve() != null ? incidencia.getUsuarioResuelve().getNombre() : null)
                .fechaResolucion(incidencia.getFechaResolucion())
                .build();
    }
}
