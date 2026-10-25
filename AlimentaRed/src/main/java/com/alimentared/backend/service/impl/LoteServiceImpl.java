package com.alimentared.backend.service.impl;

import com.alimentared.backend.dto.request.CancelacionRequest;
import com.alimentared.backend.dto.request.LoteFiltroRequest;
import com.alimentared.backend.dto.request.LoteRequest;
import com.alimentared.backend.dto.response.HistorialEstadoResponse;
import com.alimentared.backend.dto.response.LoteResponse;
import com.alimentared.backend.dto.response.PageResponse;
import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.Organizacion;
import com.alimentared.backend.entity.TipoAlimento;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.EntidadAuditada;
import com.alimentared.backend.entity.enums.EstadoAlimento;
import com.alimentared.backend.entity.enums.EstadoAsignacion;
import com.alimentared.backend.entity.enums.EstadoLote;
import com.alimentared.backend.entity.enums.EstadoSolicitud;
import com.alimentared.backend.entity.enums.Rol;
import com.alimentared.backend.exception.AccountBlockedException;
import com.alimentared.backend.exception.BusinessRuleException;
import com.alimentared.backend.exception.ForbiddenOperationException;
import com.alimentared.backend.exception.ResourceNotFoundException;
import com.alimentared.backend.mapper.LoteMapper;
import com.alimentared.backend.repository.AsignacionRepository;
import com.alimentared.backend.repository.LoteRepository;
import com.alimentared.backend.repository.SolicitudRepository;
import com.alimentared.backend.repository.TipoAlimentoRepository;
import com.alimentared.backend.service.AuditoriaService;
import com.alimentared.backend.service.InventarioService;
import com.alimentared.backend.service.LoteService;
import com.alimentared.backend.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static com.alimentared.backend.repository.specification.LoteSpecifications.*;
import static com.alimentared.backend.repository.specification.SpecificationUtils.todas;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoteServiceImpl implements LoteService {

    private static final List<EstadoLote> ESTADOS_QUE_VENCEN = List.of(EstadoLote.BORRADOR, EstadoLote.DISPONIBLE);

    private final LoteRepository loteRepository;
    private final TipoAlimentoRepository tipoAlimentoRepository;
    private final SolicitudRepository solicitudRepository;
    private final AsignacionRepository asignacionRepository;
    private final InventarioService inventarioService;
    private final SolicitudService solicitudService;
    private final AuditoriaService auditoriaService;
    private final AccesoHelper accesoHelper;
    private final LoteMapper loteMapper;
    private final Clock clock;

    @Override
    @Transactional
    public LoteResponse publicarLote(LoteRequest request, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        accesoHelper.validarRol(usuario, Rol.DONANTE);
        Organizacion donante = accesoHelper.organizacionDe(usuario);
        if (donante.isBloqueada()) {
            throw new AccountBlockedException("La organización está suspendida por acumulación de strikes");
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        validarDatosLote(request, ahora);

        Lote lote = loteMapper.toEntity(request, buscarCategoria(request.categoriaId()), donante);
        lote.setFechaRegistro(ahora);
        // RF03: una organización sin verificar solo puede guardar el lote como borrador.
        lote.setEstado(donante.isVerificada() ? EstadoLote.DISPONIBLE : EstadoLote.BORRADOR);
        lote = loteRepository.save(lote);

        auditoriaService.registrar(EntidadAuditada.LOTE, lote.getIdLote(), null, lote.getEstado(), usuario,
                donante.isVerificada() ? "Lote publicado"
                        : "Lote guardado como borrador: organización pendiente de verificación");
        return loteMapper.toResponse(lote, true);
    }

    @Override
    @Transactional
    public LoteResponse actualizarLote(Long loteId, LoteRequest request, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        Lote lote = inventarioService.bloquearLote(loteId);
        validarDonantePropietario(usuario, lote);
        if (lote.getEstado() != EstadoLote.BORRADOR && lote.getEstado() != EstadoLote.DISPONIBLE) {
            throw new BusinessRuleException("Solo se pueden modificar lotes en borrador o disponibles");
        }
        boolean yaAsignado = lote.getCantidadDisponible().compareTo(lote.getCantidadInicial()) != 0;
        if (yaAsignado || solicitudRepository.existsByLote_IdLoteAndEstadoIn(loteId, EstadoSolicitud.ACTIVOS)) {
            throw new BusinessRuleException("No se puede modificar un lote con solicitudes o asignaciones registradas");
        }
        validarDatosLote(request, LocalDateTime.now(clock));
        loteMapper.updateEntity(lote, request, buscarCategoria(request.categoriaId()));
        return loteMapper.toResponse(lote, true);
    }

    @Override
    @Transactional
    public LoteResponse publicarBorrador(Long loteId, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        Lote lote = inventarioService.bloquearLote(loteId);
        validarDonantePropietario(usuario, lote);
        if (lote.getEstado() != EstadoLote.BORRADOR) {
            throw new BusinessRuleException("RF03", "Solo se pueden publicar lotes en estado BORRADOR");
        }
        accesoHelper.validarOrganizacionOperativa(lote.getDonante());
        if (lote.estaVencido(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("RN01", "El lote ya superó su fecha límite de consumo");
        }
        inventarioService.cambiarEstado(lote, EstadoLote.DISPONIBLE, usuario, "Publicación confirmada por el donante");
        return loteMapper.toResponse(lote, true);
    }

    @Override
    @Transactional
    public LoteResponse cancelarLote(Long loteId, CancelacionRequest request, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        Lote lote = inventarioService.bloquearLote(loteId);
        accesoHelper.validarResponsableDelLote(usuario, lote);
        switch (lote.getEstado()) {
            case ENTREGADO -> throw new BusinessRuleException("RF08", "No se puede cancelar un lote que ya fue entregado");
            case CANCELADO -> throw new BusinessRuleException("RF08", "El lote ya se encuentra cancelado");
            default -> {
                // Estados cancelables.
            }
        }
        if (asignacionRepository.existsBySolicitud_Lote_IdLoteAndEstadoIn(loteId, EstadoAsignacion.ABIERTOS)) {
            throw new BusinessRuleException("RF08",
                    "El lote tiene asignaciones activas; registre una incidencia para que la coordinación la gestione");
        }
        String motivo = request.motivo().trim();
        solicitudService.cerrarSolicitudesPendientes(loteId, EstadoSolicitud.RECHAZADA, usuario, "Lote cancelado: " + motivo);
        inventarioService.cambiarEstado(lote, EstadoLote.CANCELADO, usuario, motivo);
        return loteMapper.toResponse(lote, true);
    }

    @Override
    @Transactional(readOnly = true)
    public LoteResponse obtenerLote(Long loteId, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        Lote lote = buscar(loteId);
        boolean gestor = accesoHelper.esCoordinacion(usuario) || accesoHelper.esDonanteResponsable(usuario, lote);
        if (lote.getEstado() == EstadoLote.BORRADOR && !gestor) {
            throw new ResourceNotFoundException("Lote", loteId);
        }
        return loteMapper.toResponse(lote, gestor || esReceptoraAsignada(usuario, lote));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoteResponse> filtrarLotes(LoteFiltroRequest filtro, Pageable pageable, Long usuarioId) {
        if (filtro.fechaDesde() != null && filtro.fechaHasta() != null && filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw new BusinessRuleException("RF04", "La fecha inicial del filtro no puede ser posterior a la final");
        }
        Usuario usuario = accesoHelper.usuario(usuarioId);
        LocalDateTime ahora = LocalDateTime.now(clock);

        Specification<Lote> spec = todas(
                conRelaciones(),
                categoria(filtro.categoriaId()),
                ubicacionContiene(filtro.ubicacion()),
                caducaDesde(filtro.fechaDesde() != null ? filtro.fechaDesde().atStartOfDay() : null),
                caducaAntesDe(filtro.fechaHasta() != null ? filtro.fechaHasta().plusDays(1).atStartOfDay() : null));

        if (accesoHelper.esCoordinacion(usuario)) {
            spec = spec.and(todas(estado(filtro.estado())));
        } else {
            // Catálogo público: solo lotes disponibles, vigentes y con inventario en tiempo real.
            spec = spec.and(todas(estado(EstadoLote.DISPONIBLE), noVencido(ahora), conInventario()));
        }
        Page<Lote> page = loteRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(loteMapper::toCatalogResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoteResponse> listarMisLotes(EstadoLote estado, Pageable pageable, Long usuarioId) {
        Organizacion donante = accesoHelper.organizacionDe(accesoHelper.usuario(usuarioId));
        Page<Lote> page = estado == null
                ? loteRepository.findByDonante_IdOrganizacion(donante.getIdOrganizacion(), pageable)
                : loteRepository.findByDonante_IdOrganizacionAndEstado(donante.getIdOrganizacion(), estado, pageable);
        return PageResponse.from(page.map(lote -> loteMapper.toResponse(lote, true)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEstadoResponse> obtenerHistorial(Long loteId, Long usuarioId) {
        Usuario usuario = accesoHelper.usuario(usuarioId);
        accesoHelper.validarResponsableDelLote(usuario, buscar(loteId));
        return auditoriaService.listar(EntidadAuditada.LOTE, loteId);
    }

    @Override
    @Transactional
    @Scheduled(cron = "${alimentared.cron.vencimientos:0 */5 * * * *}")
    public void marcarLotesVencidos() {
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<Lote> vencidos = loteRepository.findByEstadoInAndFechaCaducidadBefore(ESTADOS_QUE_VENCEN, ahora);
        for (Lote lote : vencidos) {
            solicitudService.cerrarSolicitudesPendientes(lote.getIdLote(), EstadoSolicitud.EXPIRADA, null,
                    "RN01: el lote alcanzó su fecha límite de consumo");
            inventarioService.cambiarEstado(lote, EstadoLote.VENCIDO, null, "RN01: fecha límite de consumo superada");
        }
        if (!vencidos.isEmpty()) {
            log.info("{} lotes marcados como vencidos", vencidos.size());
        }
    }

    /** RF03, RN05 y RN10: coherencia de fecha, estado del alimento, unidad mínima y múltiplo. */
    private void validarDatosLote(LoteRequest request, LocalDateTime ahora) {
        if (!request.fechaCaducidad().isAfter(ahora)) {
            throw new BusinessRuleException("RF03", "La fecha límite de consumo debe ser posterior a la fecha actual");
        }
        if (request.estadoAlimento() == EstadoAlimento.PODRIDO) {
            throw new BusinessRuleException("RF03", "No se pueden publicar alimentos en estado PODRIDO");
        }
        if (request.unidadMinima().compareTo(request.cantidad()) > 0) {
            throw new BusinessRuleException("RN05", "La unidad mínima no puede superar la cantidad del lote");
        }
        if (request.multiplo().compareTo(request.cantidad()) > 0) {
            throw new BusinessRuleException("RN10", "El múltiplo no puede superar la cantidad del lote");
        }
        if (request.unidadMinima().remainder(request.multiplo()).signum() != 0) {
            throw new BusinessRuleException("RN10", "La unidad mínima debe ser múltiplo del paso de fraccionamiento");
        }
    }

    private void validarDonantePropietario(Usuario usuario, Lote lote) {
        if (!accesoHelper.esDonanteResponsable(usuario, lote)) {
            throw new ForbiddenOperationException("Solo el donante responsable puede modificar este lote");
        }
    }

    private boolean esReceptoraAsignada(Usuario usuario, Lote lote) {
        return usuario.getRol() == Rol.RECEPTORA && usuario.getOrganizacion() != null
                && asignacionRepository.existsBySolicitud_Lote_IdLoteAndSolicitud_Organizacion_IdOrganizacion(
                lote.getIdLote(), usuario.getOrganizacion().getIdOrganizacion());
    }

    private TipoAlimento buscarCategoria(Long categoriaId) {
        return tipoAlimentoRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", categoriaId));
    }

    private Lote buscar(Long loteId) {
        return loteRepository.findById(loteId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", loteId));
    }
}
