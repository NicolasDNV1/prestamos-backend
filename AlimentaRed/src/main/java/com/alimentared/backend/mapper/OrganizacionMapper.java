package com.alimentared.backend.mapper;

import com.alimentared.backend.dto.request.OrganizacionRequest;
import com.alimentared.backend.dto.response.OrganizacionResponse;
import com.alimentared.backend.dto.response.StrikeResponse;
import com.alimentared.backend.entity.Organizacion;
import com.alimentared.backend.entity.Strike;
import org.springframework.stereotype.Component;

@Component
public class OrganizacionMapper {

    public Organizacion toEntity(OrganizacionRequest request) {
        Organizacion organizacion = new Organizacion();
        updateEntity(organizacion, request);
        return organizacion;
    }

    public void updateEntity(Organizacion organizacion, OrganizacionRequest request) {
        organizacion.setNit(request.nit().trim());
        organizacion.setNombre(request.nombre().trim());
        organizacion.setUbicacion(request.ubicacion());
        organizacion.setCorreo(request.correo().trim().toLowerCase());
        organizacion.setTelefono(request.telefono());
    }

    public OrganizacionResponse toResponse(Organizacion organizacion) {
        if (organizacion == null) {
            return null;
        }
        return OrganizacionResponse.builder()
                .id(organizacion.getIdOrganizacion())
                .nit(organizacion.getNit())
                .nombre(organizacion.getNombre())
                .ubicacion(organizacion.getUbicacion())
                .correo(organizacion.getCorreo())
                .telefono(organizacion.getTelefono())
                .verificada(organizacion.isVerificada())
                .bloqueada(organizacion.isBloqueada())
                .strikesVigentes(organizacion.getStrikesVigentes())
                .fechaRegistro(organizacion.getFechaRegistro())
                .build();
    }

    public StrikeResponse toStrikeResponse(Strike strike) {
        return StrikeResponse.builder()
                .id(strike.getIdStrike())
                .organizacionId(strike.getOrganizacion().getIdOrganizacion())
                .organizacionNombre(strike.getOrganizacion().getNombre())
                .asignacionId(strike.getAsignacion() != null ? strike.getAsignacion().getIdAsignacion() : null)
                .nombreStrike(strike.getNombreStrike())
                .detalles(strike.getDetalles())
                .fechaRegistro(strike.getFechaRegistro())
                .vigente(strike.isVigente())
                .justificacionApelacion(strike.getJustificacionApelacion())
                .fechaApelacion(strike.getFechaApelacion())
                .build();
    }
}
