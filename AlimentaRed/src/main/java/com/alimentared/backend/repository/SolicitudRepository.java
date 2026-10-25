package com.alimentared.backend.repository;

import com.alimentared.backend.entity.Solicitud;
import com.alimentared.backend.entity.enums.EstadoSolicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long>, JpaSpecificationExecutor<Solicitud> {

    /** RN07: conteo de solicitudes activas de una organización. */
    long countByOrganizacion_IdOrganizacionAndEstadoIn(Long idOrganizacion, Collection<EstadoSolicitud> estados);

    boolean existsByLote_IdLoteAndOrganizacion_IdOrganizacionAndEstadoIn(
            Long idLote, Long idOrganizacion, Collection<EstadoSolicitud> estados);

    boolean existsByLote_IdLoteAndEstadoIn(Long idLote, Collection<EstadoSolicitud> estados);

    List<Solicitud> findByLote_IdLoteAndEstadoOrderByFechaCreacionAsc(Long idLote, EstadoSolicitud estado);

    List<Solicitud> findByOrganizacion_IdOrganizacionAndEstado(Long idOrganizacion, EstadoSolicitud estado);

    /** RN06: solicitudes pendientes cuyo SLA ya venció. */
    List<Solicitud> findByEstadoAndFechaCreacionBefore(EstadoSolicitud estado, LocalDateTime fecha);

    /** RN09: lotes con solicitudes pendientes cuya ventana de empate ya cerró. */
    @Query("""
            select distinct s.lote.idLote from Solicitud s
            where s.estado = :estado and s.enEmpate = false and s.fechaCreacion <= :limite
            """)
    List<Long> findLotesConVentanaCerrada(@Param("estado") EstadoSolicitud estado,
                                          @Param("limite") LocalDateTime limite);

    @EntityGraph(attributePaths = {"lote", "lote.categoria", "organizacion"})
    Page<Solicitud> findByOrganizacion_IdOrganizacion(Long idOrganizacion, Pageable pageable);

    @EntityGraph(attributePaths = {"lote", "lote.categoria", "organizacion"})
    Page<Solicitud> findByLote_IdLote(Long idLote, Pageable pageable);

    @EntityGraph(attributePaths = {"lote", "lote.categoria", "organizacion"})
    Page<Solicitud> findByEstado(EstadoSolicitud estado, Pageable pageable);

    @EntityGraph(attributePaths = {"lote", "lote.categoria", "organizacion"})
    Page<Solicitud> findAllBy(Pageable pageable);
}
