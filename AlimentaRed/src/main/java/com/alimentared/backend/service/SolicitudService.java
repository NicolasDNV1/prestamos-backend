package com.alimentared.backend.service;

import com.alimentared.backend.dto.request.CancelacionRequest;
import com.alimentared.backend.dto.request.ResolverSolicitudRequest;
import com.alimentared.backend.dto.request.SolicitudRequest;
import com.alimentared.backend.dto.response.HistorialEstadoResponse;
import com.alimentared.backend.dto.response.PageResponse;
import com.alimentared.backend.dto.response.SolicitudResponse;
import com.alimentared.backend.entity.Solicitud;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.EstadoSolicitud;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SolicitudService {

    SolicitudResponse crearSolicitud(SolicitudRequest request, Long usuarioId);

    SolicitudResponse resolverSolicitud(Long solicitudId, ResolverSolicitudRequest request, Long usuarioId);

    SolicitudResponse cancelarSolicitud(Long solicitudId, CancelacionRequest request, Long usuarioId);

    SolicitudResponse obtenerSolicitud(Long solicitudId, Long usuarioId);

    PageResponse<SolicitudResponse> obtenerSolicitudesPorOrganizacion(Long usuarioId, Pageable pageable);

    PageResponse<SolicitudResponse> listarPorLote(Long loteId, Long usuarioId, Pageable pageable);

    PageResponse<SolicitudResponse> listarSolicitudes(EstadoSolicitud estado, Pageable pageable);

    long contarSolicitudesActivas(Long usuarioId);

    List<HistorialEstadoResponse> obtenerHistorial(Long solicitudId, Long usuarioId);

    /** Cambio de estado con trazabilidad (RN04), usado también por otros servicios. */
    void actualizarEstado(Solicitud solicitud, EstadoSolicitud nuevoEstado, Usuario responsable, String motivo);

    void cerrarSolicitudesPendientes(Long loteId, EstadoSolicitud estadoFinal, Usuario responsable, String motivo);

    /** Tarea programada RN09: auto-asignación FCFS o empate pendiente de decisión del donante. */
    void procesarVentanaEmpate();

    /** Tarea programada RN06: expira las solicitudes pendientes que superan el SLA de 1 hora. */
    void verificarSlaExpiracion();
}
