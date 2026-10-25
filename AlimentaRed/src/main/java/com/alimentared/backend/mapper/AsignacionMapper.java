package com.alimentared.backend.mapper;

import com.alimentared.backend.dto.response.AsignacionResponse;
import com.alimentared.backend.dto.response.EntregaResponse;
import com.alimentared.backend.entity.Asignacion;
import com.alimentared.backend.entity.Entrega;
import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.Organizacion;
import com.alimentared.backend.entity.Solicitud;
import org.springframework.stereotype.Component;

@Component
public class AsignacionMapper {

    /** Al existir asignación se revelan las instrucciones de recogida y el contacto (RF06). */
    public AsignacionResponse toResponse(Asignacion asignacion) {
        Solicitud solicitud = asignacion.getSolicitud();
        Lote lote = solicitud.getLote();
        Organizacion receptora = solicitud.getOrganizacion();
        Organizacion donante = lote.getDonante();
        return AsignacionResponse.builder()
                .id(asignacion.getIdAsignacion())
                .solicitudId(solicitud.getIdSolicitud())
                .loteId(lote.getIdLote())
                .loteNombre(lote.getNombre())
                .unidadMedida(lote.getCategoria().getUnidadMedida())
                .organizacionId(receptora.getIdOrganizacion())
                .organizacionNombre(receptora.getNombre())
                .donanteId(donante.getIdOrganizacion())
                .donanteNombre(donante.getNombre())
                .cantidadAsignada(asignacion.getCantidadAsignada())
                .cantidadRetirada(asignacion.getCantidadRetirada())
                .cantidadPendiente(asignacion.getCantidadPendiente())
                .estado(asignacion.getEstado())
                .autoAsignada(asignacion.isAutoAsignada())
                .fechaAsignacion(asignacion.getFechaAsignacion())
                .fechaLimiteRetiro(asignacion.getFechaLimiteRetiro())
                .fechaCierre(asignacion.getFechaCierre())
                .ubicacion(lote.getUbicacion())
                .instruccionesRecogida(lote.getInstruccionesRecogida())
                .contacto(lote.getContacto())
                .build();
    }

    public EntregaResponse toEntregaResponse(Entrega entrega) {
        Solicitud solicitud = entrega.getSolicitud();
        Lote lote = solicitud.getLote();
        Organizacion receptora = solicitud.getOrganizacion();
        Organizacion donante = lote.getDonante();
        return EntregaResponse.builder()
                .id(entrega.getIdEntrega())
                .asignacionId(entrega.getAsignacion().getIdAsignacion())
                .solicitudId(solicitud.getIdSolicitud())
                .loteId(lote.getIdLote())
                .loteNombre(lote.getNombre())
                .categoriaNombre(lote.getCategoria().getNombre())
                .unidadMedida(lote.getCategoria().getUnidadMedida())
                .donanteId(donante.getIdOrganizacion())
                .donanteNombre(donante.getNombre())
                .receptoraId(receptora.getIdOrganizacion())
                .receptoraNombre(receptora.getNombre())
                .cantidadRetirada(entrega.getCantidadRetirada())
                .estado(entrega.getEstado())
                .fechaEntrega(entrega.getFechaEntrega())
                .observaciones(entrega.getObservaciones())
                .responsable(entrega.getUsuarioResponsable().getNombre())
                .fueraDeFranja(entrega.isFueraDeFranja())
                .build();
    }
}
