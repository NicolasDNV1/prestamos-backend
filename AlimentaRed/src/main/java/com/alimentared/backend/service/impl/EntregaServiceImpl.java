package com.alimentared.backend.service.impl;

import com.alimentared.backend.dto.request.ConfirmarEntregaRequest;
import com.alimentared.backend.dto.request.NovedadEntregaRequest;
import com.alimentared.backend.dto.response.AsignacionResponse;
import com.alimentared.backend.dto.response.EntregaResponse;
import com.alimentared.backend.dto.response.PageResponse;
import com.alimentared.backend.entity.Asignacion;
import com.alimentared.backend.entity.Entrega;
import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.Solicitud;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.EntidadAuditada;
import com.alimentared.backend.entity.enums.EstadoAsignacion;
import com.alimentared.backend.entity.enums.EstadoEntrega;
import com.alimentared.backend.entity.enums.EstadoSolicitud;
import com.alimentared.backend.exception.BusinessRuleException;
import com.alimentared.backend.exception.ForbiddenOperationException;
import com.alimentared.backend.exception.ResourceNotFoundException;
import com.alimentared.backend.mapper.AsignacionMapper;
import com.alimentared.backend.repository.AsignacionRepository;
import com.alimentared.backend.repository.EntregaRepository;
import com.alimentared.backend.repository.StrikeRepository;
import com.alimentared.backend.service.AuditoriaService;
import com.alimentared.backend.service.EntregaService;
import com.alimentared.backend.service.InventarioService;
import com.alimentared.backend.service.SolicitudService;
import com.alimentared.backend.service.StrikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EntregaServiceImpl implements EntregaService {

    private final AsignacionRepository asignacionRepository;
    private final EntregaRepository entregaRepository;
    private final StrikeRepository strikeRepository;
    private final InventarioService inventarioService;
    private final SolicitudService solicitudService;
    private final StrikeService strikeService;
    private final AuditoriaService auditoriaService;
    private final AccesoHelper accesoHelper;
    private final AsignacionMapper asignacionMapper;
    private final Clock clock;

    @Override
    @Transactional
    public EntregaResponse confirmarRecepcion(Long asignacionId, ConfirmarEntregaRequest request, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        Asignacion asignacion = buscar(asignacionId);
        Solicitud solicitud = asignacion.getSolicitud();
        Lote lote = inventarioService.bloquearLote(solicitud.getLote().getIdLote());
        // Solo el donante responsable o un coordinador autorizado confirman la salida del alimento.
        accesoHelper.validarResponsableDelLote(usuario, lote);
        validarAbierta(asignacion);

        BigDecimal cantidad = request.cantidadRetirada();
        BigDecimal pendiente = asignacion.getCantidadPendiente();
        if (cantidad.compareTo(pendiente) > 0) {
            throw new BusinessRuleException("RN02", "La cantidad retirada (%s) supera la pendiente de la asignación (%s)"
                    .formatted(cantidad.stripTrailingZeros().toPlainString(), pendiente.stripTrailingZeros().toPlainString()));
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        boolean fueraDeFranja = ahora.isAfter(asignacion.getFechaLimiteRetiro());
        asignacion.setCantidadRetirada(asignacion.getCantidadRetirada().add(cantidad));
        boolean completa = asignacion.getCantidadPendiente().signum() == 0;

        Entrega entrega = entregaRepository.save(Entrega.builder()
                .asignacion(asignacion)
                .solicitud(solicitud)
                .cantidadRetirada(cantidad)
                .estado(completa ? EstadoEntrega.COMPLETA : EstadoEntrega.PARCIAL)
                .fechaEntrega(ahora)
                .observaciones(request.observaciones())
                .usuarioResponsable(usuario)
                .fueraDeFranja(fueraDeFranja)
                .build());

        EstadoAsignacion nuevoEstado = completa ? EstadoAsignacion.COMPLETADA : EstadoAsignacion.RETIRO_PARCIAL;
        cambiarEstado(asignacion, nuevoEstado, usuario, "Retiro de %s registrado"
                .formatted(cantidad.stripTrailingZeros().toPlainString()));
        if (completa) {
            asignacion.setFechaCierre(ahora);
            solicitudService.actualizarEstado(solicitud, EstadoSolicitud.COMPLETADA, usuario, "Entrega completada");
            inventarioService.evaluarCierreLote(lote, usuario);
        }

        // RF07: retiro fuera de la franja horaria = strike automático (uno por asignación).
        if (fueraDeFranja && !strikeRepository.existsByAsignacion_IdAsignacion(asignacionId)) {
            strikeService.registrarStrike(solicitud.getOrganizacion(), asignacion, "Retiro fuera de franja horaria",
                    "Retiro registrado el %s; la franja vencía el %s".formatted(ahora, asignacion.getFechaLimiteRetiro()),
                    usuario);
        }
        return asignacionMapper.toEntregaResponse(entrega);
    }

    @Override
    @Transactional
    public AsignacionResponse registrarNoRecogida(Long asignacionId, NovedadEntregaRequest request, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        Asignacion asignacion = buscar(asignacionId);
        Solicitud solicitud = asignacion.getSolicitud();
        Lote lote = inventarioService.bloquearLote(solicitud.getLote().getIdLote());
        accesoHelper.validarResponsableDelLote(usuario, lote);
        validarAbierta(asignacion);

        LocalDateTime ahora = LocalDateTime.now(clock);
        if (!accesoHelper.esCoordinacion(usuario) && !ahora.isAfter(asignacion.getFechaLimiteRetiro())) {
            throw new BusinessRuleException("RN08", "Solo puede reportarse como no recogida después de la franja de retiro (%s)"
                    .formatted(asignacion.getFechaLimiteRetiro()));
        }
        String observaciones = request.observaciones().trim();
        inventarioService.reponer(lote, asignacion.getCantidadPendiente(), usuario,
                "Restock por asignación no recogida #" + asignacionId);
        cerrarAsignacion(asignacion, EstadoAsignacion.NO_RECOGIDA, usuario, "No recogida: " + observaciones, ahora);
        strikeService.registrarStrike(solicitud.getOrganizacion(), asignacion, "Asignación no recogida",
                observaciones, usuario);
        return asignacionMapper.toResponse(asignacion);
    }

    @Override
    @Transactional
    public AsignacionResponse restockSobrante(Long asignacionId, NovedadEntregaRequest request, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        if (!accesoHelper.esCoordinacion(usuario)) {
            throw new ForbiddenOperationException("Solo la coordinación puede ejecutar el restock del sobrante");
        }
        Asignacion asignacion = buscar(asignacionId);
        Lote lote = inventarioService.bloquearLote(asignacion.getSolicitud().getLote().getIdLote());
        validarAbierta(asignacion);

        String observaciones = request.observaciones().trim();
        inventarioService.reponer(lote, asignacion.getCantidadPendiente(), usuario,
                "RN11: restock del sobrante de la asignación #%d. %s".formatted(asignacionId, observaciones));
        cerrarAsignacion(asignacion, EstadoAsignacion.CERRADA_CON_RESTOCK, usuario,
                "Restock del sobrante: " + observaciones, LocalDateTime.now(clock));
        return asignacionMapper.toResponse(asignacion);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AsignacionResponse> listarAsignaciones(Long usuarioId, Pageable pageable) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        Page<Asignacion> page = switch (usuario.getRol()) {
            case RECEPTORA -> asignacionRepository.findBySolicitud_Organizacion_IdOrganizacion(
                    accesoHelper.organizacionDe(usuario).getIdOrganizacion(), pageable);
            case DONANTE -> asignacionRepository.findBySolicitud_Lote_Donante_IdOrganizacion(
                    accesoHelper.organizacionDe(usuario).getIdOrganizacion(), pageable);
            case COORDINADOR, ADMINISTRADOR -> asignacionRepository.findAllBy(pageable);
        };
        return PageResponse.from(page.map(asignacionMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AsignacionResponse obtenerAsignacion(Long asignacionId, Long usuarioId) {
        Asignacion asignacion = buscar(asignacionId);
        validarVisibilidad(accesoHelper.usuario(usuarioId), asignacion);
        return asignacionMapper.toResponse(asignacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntregaResponse> listarEntregas(Long asignacionId, Long usuarioId) {
        validarVisibilidad(accesoHelper.usuario(usuarioId), buscar(asignacionId));
        return entregaRepository.findByAsignacion_IdAsignacionOrderByFechaEntregaAsc(asignacionId).stream()
                .map(asignacionMapper::toEntregaResponse)
                .toList();
    }

    /** Cierra la asignación; la solicitud queda completada si hubo algún retiro, si no, cancelada. */
    private void cerrarAsignacion(Asignacion asignacion, EstadoAsignacion estadoFinal, Usuario usuario,
                                  String motivo, LocalDateTime ahora) {
        cambiarEstado(asignacion, estadoFinal, usuario, motivo);
        asignacion.setFechaCierre(ahora);
        boolean huboRetiro = asignacion.getCantidadRetirada().signum() > 0;
        solicitudService.actualizarEstado(asignacion.getSolicitud(),
                huboRetiro ? EstadoSolicitud.COMPLETADA : EstadoSolicitud.CANCELADO, usuario, motivo);
    }

    private void cambiarEstado(Asignacion asignacion, EstadoAsignacion nuevoEstado, Usuario usuario, String motivo) {
        EstadoAsignacion anterior = asignacion.getEstado();
        asignacion.setEstado(nuevoEstado);
        auditoriaService.registrar(EntidadAuditada.ASIGNACION, asignacion.getIdAsignacion(), anterior, nuevoEstado,
                usuario, motivo);
    }

    private void validarAbierta(Asignacion asignacion) {
        if (!EstadoAsignacion.ABIERTOS.contains(asignacion.getEstado())) {
            throw new BusinessRuleException("RF07",
                    "La asignación ya está cerrada (estado: %s)".formatted(asignacion.getEstado()));
        }
    }

    private void validarVisibilidad(Usuario usuario, Asignacion asignacion) {
        Solicitud solicitud = asignacion.getSolicitud();
        boolean permitido = accesoHelper.esCoordinacion(usuario)
                || accesoHelper.perteneceA(usuario, solicitud.getOrganizacion())
                || accesoHelper.esDonanteResponsable(usuario, solicitud.getLote());
        if (!permitido) {
            throw new ForbiddenOperationException("No tiene acceso a esta asignación");
        }
    }

    private Asignacion buscar(Long asignacionId) {
        return asignacionRepository.findById(asignacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación", asignacionId));
    }
}
