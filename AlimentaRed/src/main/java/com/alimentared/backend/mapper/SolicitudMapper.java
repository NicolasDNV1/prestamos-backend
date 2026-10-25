package com.alimentared.backend.mapper;

import com.alimentared.backend.config.AlimentaRedProperties;
import com.alimentared.backend.dto.response.SolicitudResponse;
import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.Organizacion;
import com.alimentared.backend.entity.Solicitud;
import com.alimentared.backend.entity.enums.EstadoSolicitud;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SolicitudMapper {

    private final AlimentaRedProperties properties;

    public Solicitud toEntity(Lote lote, Organizacion organizacion, BigDecimal cantidad, LocalDateTime fechaCreacion) {
        return Solicitud.builder()
                .lote(lote)
                .organizacion(organizacion)
                .cantidadSolicitada(cantidad)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaCreacion(fechaCreacion)
                .build();
    }

    public SolicitudResponse toResponse(Solicitud solicitud) {
        Lote lote = solicitud.getLote();
        Organizacion organizacion = solicitud.getOrganizacion();
        return SolicitudResponse.builder()
                .id(solicitud.getIdSolicitud())
                .loteId(lote.getIdLote())
                .loteNombre(lote.getNombre())
                .unidadMedida(lote.getCategoria().getUnidadMedida())
                .organizacionId(organizacion.getIdOrganizacion())
                .organizacionNombre(organizacion.getNombre())
                .cantidadSolicitada(solicitud.getCantidadSolicitada())
                .estado(solicitud.getEstado())
                .enEmpate(solicitud.isEnEmpate())
                .fechaCreacion(solicitud.getFechaCreacion())
                .fechaLimiteConfirmacion(solicitud.getEstado() == EstadoSolicitud.PENDIENTE
                        ? solicitud.getFechaCreacion().plusMinutes(properties.reglas().slaConfirmacionMinutos())
                        : null)
                .fechaResolucion(solicitud.getFechaResolucion())
                .motivoResolucion(solicitud.getMotivoResolucion())
                .build();
    }
}
