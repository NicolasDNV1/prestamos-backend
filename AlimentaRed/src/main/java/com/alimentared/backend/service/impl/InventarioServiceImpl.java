package com.alimentared.backend.service.impl;

import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.Usuario;
import com.alimentared.backend.entity.enums.EntidadAuditada;
import com.alimentared.backend.entity.enums.EstadoAlimento;
import com.alimentared.backend.entity.enums.EstadoAsignacion;
import com.alimentared.backend.entity.enums.EstadoLote;
import com.alimentared.backend.exception.BusinessRuleException;
import com.alimentared.backend.exception.ResourceNotFoundException;
import com.alimentared.backend.repository.AsignacionRepository;
import com.alimentared.backend.repository.LoteRepository;
import com.alimentared.backend.service.AuditoriaService;
import com.alimentared.backend.service.InventarioService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class InventarioServiceImpl implements InventarioService {

    private final LoteRepository loteRepository;
    private final AsignacionRepository asignacionRepository;
    private final AuditoriaService auditoriaService;
    private final EntityManager entityManager;
    private final Clock clock;

    @Override
    public Lote bloquearLote(Long idLote) {
        Lote lote = loteRepository.findById(idLote)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", idLote));
        // SELECT ... FOR UPDATE: serializa asignaciones concurrentes sobre el mismo lote (integridad RN02).
        entityManager.refresh(lote, LockModeType.PESSIMISTIC_WRITE);
        return lote;
    }

    /** RN01: no se puede solicitar un lote vencido, cancelado o entregado. */
    @Override
    public void validarLoteSolicitable(Lote lote, LocalDateTime ahora) {
        switch (lote.getEstado()) {
            case CANCELADO -> throw new BusinessRuleException("RN01", "El lote fue cancelado y no admite solicitudes");
            case ENTREGADO -> throw new BusinessRuleException("RN01", "El lote ya fue entregado");
            case VENCIDO -> throw new BusinessRuleException("RN01", "El lote está vencido");
            case INCIDENCIA -> throw new BusinessRuleException("RN01", "El lote está bloqueado por una incidencia");
            case BORRADOR -> throw new BusinessRuleException("RN01", "El lote aún no ha sido publicado");
            case ASIGNADO -> throw new BusinessRuleException("RN02", "El lote no tiene inventario disponible");
            case DISPONIBLE -> {
                // Estado válido para solicitar.
            }
        }
        if (lote.estaVencido(ahora)) {
            throw new BusinessRuleException("RN01",
                    "El lote está vencido (fecha límite: %s)".formatted(lote.getFechaCaducidad()));
        }
        if (lote.getEstadoAlimento() == EstadoAlimento.PODRIDO) {
            throw new BusinessRuleException("RN01", "El alimento del lote no es apto para consumo");
        }
    }

    /**
     * RN02, RN05 y RN10: la cantidad debe ser positiva, no superar la disponible, respetar la unidad
     * mínima y el múltiplo, y no dejar un remanente inferior a la unidad mínima.
     * Retirar exactamente todo lo disponible siempre es válido porque no deja saldos inútiles.
     */
    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void validarFraccionamiento(Lote lote, BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new BusinessRuleException("RN05", "La cantidad solicitada debe ser mayor a cero");
        }
        BigDecimal disponible = lote.getCantidadDisponible();
        if (cantidad.compareTo(disponible) > 0) {
            throw new BusinessRuleException("RN02", "La cantidad solicitada (%s) supera la disponible del lote (%s)"
                    .formatted(texto(cantidad), texto(disponible)));
        }
        if (cantidad.compareTo(disponible) == 0) {
            return;
        }
        if (cantidad.compareTo(lote.getUnidadMinima()) < 0) {
            throw new BusinessRuleException("RN05", "La cantidad mínima por solicitud es %s"
                    .formatted(texto(lote.getUnidadMinima())));
        }
        if (cantidad.remainder(lote.getMultiplo()).signum() != 0) {
            throw new BusinessRuleException("RN10", "La cantidad debe ser múltiplo de %s"
                    .formatted(texto(lote.getMultiplo())));
        }
        BigDecimal remanente = disponible.subtract(cantidad);
        if (remanente.compareTo(lote.getUnidadMinima()) < 0) {
            throw new BusinessRuleException("RN10",
                    "La solicitud dejaría un remanente inútil de %s. Solicite el total disponible (%s) o deje al menos %s"
                            .formatted(texto(remanente), texto(disponible), texto(lote.getUnidadMinima())));
        }
    }

    /** RN03: una asignación aprobada disminuye la disponibilidad de forma consistente. */
    @Override
    public void descontar(Lote lote, BigDecimal cantidad, Usuario responsable) {
        BigDecimal nuevoDisponible = lote.getCantidadDisponible().subtract(cantidad);
        if (nuevoDisponible.signum() < 0) {
            throw new BusinessRuleException("RN02", "Inventario insuficiente en el momento de la asignación");
        }
        lote.setCantidadDisponible(nuevoDisponible);
        if (nuevoDisponible.signum() == 0) {
            cambiarEstado(lote, EstadoLote.ASIGNADO, responsable, "Inventario asignado en su totalidad");
        }
    }

    /** RN11: el sobrante vuelve al catálogo; un lote agotado vuelve a estar disponible. */
    @Override
    public void reponer(Lote lote, BigDecimal cantidad, Usuario responsable, String justificacion) {
        if (cantidad == null || cantidad.signum() <= 0) {
            return;
        }
        BigDecimal nuevoDisponible = lote.getCantidadDisponible().add(cantidad);
        if (nuevoDisponible.compareTo(lote.getCantidadInicial()) > 0) {
            throw new BusinessRuleException("RN02", "El restock superaría la cantidad inicial del lote");
        }
        lote.setCantidadDisponible(nuevoDisponible);
        if (lote.getEstado() == EstadoLote.ASIGNADO) {
            EstadoLote destino = lote.estaVencido(LocalDateTime.now(clock)) ? EstadoLote.VENCIDO : EstadoLote.DISPONIBLE;
            cambiarEstado(lote, destino, responsable, justificacion);
        }
    }

    /** El lote pasa a ENTREGADO cuando no queda inventario ni asignaciones abiertas. */
    @Override
    public void evaluarCierreLote(Lote lote, Usuario responsable) {
        boolean sinInventario = lote.getCantidadDisponible().signum() == 0;
        if (lote.getEstado() == EstadoLote.ASIGNADO && sinInventario
                && !asignacionRepository.existsBySolicitud_Lote_IdLoteAndEstadoIn(
                lote.getIdLote(), EstadoAsignacion.ABIERTOS)) {
            cambiarEstado(lote, EstadoLote.ENTREGADO, responsable, "Todo el inventario del lote fue retirado");
        }
    }

    @Override
    public void cambiarEstado(Lote lote, EstadoLote nuevoEstado, Usuario responsable, String justificacion) {
        EstadoLote anterior = lote.getEstado();
        if (anterior == nuevoEstado) {
            return;
        }
        lote.setEstado(nuevoEstado);
        auditoriaService.registrar(EntidadAuditada.LOTE, lote.getIdLote(), anterior, nuevoEstado,
                responsable, justificacion);
    }

    private static String texto(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }
}
