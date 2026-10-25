package com.alimentared.backend.service.impl;

import com.alimentared.backend.dto.request.IncidenciaRequest;
import com.alimentared.backend.dto.request.ResolverIncidenciaRequest;
import com.alimentared.backend.dto.response.IncidenciaResponse;
import com.alimentared.backend.dto.response.PageResponse;
import com.alimentared.backend.entity.Asignacion;
import com.alimentared.backend.entity.Incidencia;
import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.EntidadAuditada;
import com.alimentared.backend.entity.enums.EstadoAsignacion;
import com.alimentared.backend.entity.enums.EstadoLote;
import com.alimentared.backend.entity.enums.EstadoSolicitud;
import com.alimentared.backend.entity.enums.Rol;
import com.alimentared.backend.exception.BusinessRuleException;
import com.alimentared.backend.exception.ForbiddenOperationException;
import com.alimentared.backend.exception.ResourceNotFoundException;
import com.alimentared.backend.mapper.IncidenciaMapper;
import com.alimentared.backend.repository.AsignacionRepository;
import com.alimentared.backend.repository.IncidenciaRepository;
import com.alimentared.backend.repository.LoteRepository;
import com.alimentared.backend.service.AuditoriaService;
import com.alimentared.backend.service.IncidenciaService;
import com.alimentared.backend.service.InventarioService;
import com.alimentared.backend.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IncidenciaServiceImpl implements IncidenciaService {

    private static final Set<EstadoLote> ESTADOS_CERRADOS = Set.of(EstadoLote.ENTREGADO, EstadoLote.CANCELADO);

    private final IncidenciaRepository incidenciaRepository;
    private final LoteRepository loteRepository;
    private final AsignacionRepository asignacionRepository;
    private final InventarioService inventarioService;
    private final SolicitudService solicitudService;
    private final AuditoriaService auditoriaService;
    private final AccesoHelper accesoHelper;
    private final IncidenciaMapper incidenciaMapper;
    private final Clock clock;

    /** RF08: la incidencia bloquea el lote para nuevas solicitudes y queda trazada. */
    @Override
    @Transactional
    public IncidenciaResponse reportarIncidencia(IncidenciaRequest request, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        Lote lote = inventarioService.bloquearLote(request.loteId());
        validarPuedeReportar(usuario, lote);
        if (ESTADOS_CERRADOS.contains(lote.getEstado())) {
            throw new BusinessRuleException("RF08", "No se pueden registrar incidencias sobre un lote entregado o cancelado");
        }

        EstadoLote estadoPrevio = lote.getEstado() == EstadoLote.INCIDENCIA
                ? estadoPrevioALaIncidenciaAbierta(lote.getIdLote())
                : lote.getEstado();
        String detalles = request.detalles().trim();
        Incidencia incidencia = incidenciaRepository.save(Incidencia.builder()
                .lote(lote)
                .detalles(detalles)
                .estadoLoteAnterior(estadoPrevio)
                .usuarioReporta(usuario)
                .fechaReporte(LocalDateTime.now(clock))
                .build());
        auditoriaService.registrar(EntidadAuditada.INCIDENCIA, incidencia.getIdIncidencia(), null, "ABIERTA",
                usuario, detalles);

        if (lote.getEstado() != EstadoLote.INCIDENCIA) {
            solicitudService.cerrarSolicitudesPendientes(lote.getIdLote(), EstadoSolicitud.RECHAZADA, usuario,
                    "Lote bloqueado por incidencia #" + incidencia.getIdIncidencia());
            inventarioService.cambiarEstado(lote, EstadoLote.INCIDENCIA, usuario,
                    "Incidencia #%d: %s".formatted(incidencia.getIdIncidencia(), detalles));
        }
        return incidenciaMapper.toResponse(incidencia);
    }

    @Override
    @Transactional
    public IncidenciaResponse resolverIncidencia(Long incidenciaId, ResolverIncidenciaRequest request, Long usuarioId) {
        Usuario coordinador = accesoHelper.usuario(usuarioId);
        if (!accesoHelper.esCoordinacion(coordinador)) {
            throw new ForbiddenOperationException("Solo la coordinación puede resolver incidencias");
        }
        Incidencia incidencia = incidenciaRepository.findById(incidenciaId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidencia", incidenciaId));
        if (incidencia.isResuelta()) {
            throw new BusinessRuleException("RF08", "La incidencia ya fue resuelta");
        }
        Lote lote = inventarioService.bloquearLote(incidencia.getLote().getIdLote());
        LocalDateTime ahora = LocalDateTime.now(clock);
        String resolucion = request.resolucion().trim();

        incidencia.setResuelta(true);
        incidencia.setResolucion(resolucion);
        incidencia.setUsuarioResuelve(coordinador);
        incidencia.setFechaResolucion(ahora);
        auditoriaService.registrar(EntidadAuditada.INCIDENCIA, incidenciaId, "ABIERTA", "RESUELTA", coordinador, resolucion);

        boolean otrasAbiertas = incidenciaRepository.existsByLote_IdLoteAndResueltaFalseAndIdIncidenciaNot(
                lote.getIdLote(), incidenciaId);
        if (!otrasAbiertas && lote.getEstado() == EstadoLote.INCIDENCIA) {
            if (Boolean.TRUE.equals(request.reactivarLote())) {
                reactivarLote(lote, incidencia.getEstadoLoteAnterior(), coordinador, resolucion, ahora);
            } else {
                cancelarLotePorIncidencia(lote, coordinador, resolucion);
            }
        }
        return incidenciaMapper.toResponse(incidencia);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<IncidenciaResponse> listarIncidencias(Boolean resuelta, Pageable pageable) {
        Page<Incidencia> page = resuelta == null
                ? incidenciaRepository.findAll(pageable)
                : incidenciaRepository.findByResuelta(resuelta, pageable);
        return PageResponse.from(page.map(incidenciaMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidenciaResponse> listarPorLote(Long loteId, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        Lote lote = loteRepository.findById(loteId).orElseThrow(() -> new ResourceNotFoundException("Lote", loteId));
        accesoHelper.validarResponsableDelLote(usuario, lote);
        return incidenciaRepository.findByLote_IdLoteOrderByFechaReporteDesc(loteId).stream()
                .map(incidenciaMapper::toResponse)
                .toList();
    }

    private void reactivarLote(Lote lote, EstadoLote estadoPrevio, Usuario coordinador, String resolucion,
                               LocalDateTime ahora) {
        EstadoLote destino;
        if (lote.estaVencido(ahora)) {
            destino = EstadoLote.VENCIDO;
        } else if (estadoPrevio == EstadoLote.BORRADOR) {
            destino = EstadoLote.BORRADOR;
        } else if (lote.getCantidadDisponible().signum() == 0) {
            destino = EstadoLote.ASIGNADO;
        } else {
            destino = EstadoLote.DISPONIBLE;
        }
        inventarioService.cambiarEstado(lote, destino, coordinador, "Incidencia resuelta: " + resolucion);
        inventarioService.evaluarCierreLote(lote, coordinador);
    }

    /** El alimento no es apto: se cancelan las asignaciones abiertas sin restock ni strikes. */
    private void cancelarLotePorIncidencia(Lote lote, Usuario coordinador, String resolucion) {
        String motivo = "Lote cancelado por incidencia: " + resolucion;
        List<Asignacion> abiertas = asignacionRepository.findBySolicitud_Lote_IdLoteAndEstadoIn(
                lote.getIdLote(), EstadoAsignacion.ABIERTOS);
        LocalDateTime ahora = LocalDateTime.now(clock);
        for (Asignacion asignacion : abiertas) {
            EstadoAsignacion anterior = asignacion.getEstado();
            asignacion.setEstado(EstadoAsignacion.CANCELADA);
            asignacion.setFechaCierre(ahora);
            auditoriaService.registrar(EntidadAuditada.ASIGNACION, asignacion.getIdAsignacion(), anterior,
                    EstadoAsignacion.CANCELADA, coordinador, motivo);
            EstadoSolicitud estadoSolicitud = asignacion.getCantidadRetirada().signum() > 0
                    ? EstadoSolicitud.COMPLETADA : EstadoSolicitud.CANCELADO;
            solicitudService.actualizarEstado(asignacion.getSolicitud(), estadoSolicitud, coordinador, motivo);
        }
        inventarioService.cambiarEstado(lote, EstadoLote.CANCELADO, coordinador, motivo);
    }

    private EstadoLote estadoPrevioALaIncidenciaAbierta(Long loteId) {
        return incidenciaRepository.findByLote_IdLoteOrderByFechaReporteDesc(loteId).stream()
                .filter(incidencia -> !incidencia.isResuelta())
                .map(Incidencia::getEstadoLoteAnterior)
                .findFirst()
                .orElse(EstadoLote.DISPONIBLE);
    }

    private void validarPuedeReportar(Usuario usuario, Lote lote) {
        boolean permitido = accesoHelper.esCoordinacion(usuario)
                || accesoHelper.esDonanteResponsable(usuario, lote)
                || (usuario.getRol() == Rol.RECEPTORA && usuario.getOrganizacion() != null
                && asignacionRepository.existsBySolicitud_Lote_IdLoteAndSolicitud_Organizacion_IdOrganizacion(
                lote.getIdLote(), usuario.getOrganizacion().getIdOrganizacion()));
        if (!permitido) {
            throw new ForbiddenOperationException(
                    "Solo el donante responsable, la receptora asignada o la coordinación pueden reportar incidencias");
        }
    }
}
