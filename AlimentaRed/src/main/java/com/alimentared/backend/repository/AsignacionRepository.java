package com.alimentared.backend.repository;

import com.alimentared.backend.entity.Asignacion;
import com.alimentared.backend.entity.enums.EstadoAsignacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionRepository extends JpaRepository<Asignacion, Long>, JpaSpecificationExecutor<Asignacion> {

    Optional<Asignacion> findBySolicitud_IdSolicitud(Long idSolicitud);

    boolean existsBySolicitud_Lote_IdLoteAndEstadoIn(Long idLote, Collection<EstadoAsignacion> estados);

    boolean existsBySolicitud_Lote_IdLoteAndSolicitud_Organizacion_IdOrganizacion(Long idLote, Long idOrganizacion);

    List<Asignacion> findBySolicitud_Lote_IdLoteAndEstadoIn(Long idLote, Collection<EstadoAsignacion> estados);

    @EntityGraph(attributePaths = {"solicitud", "solicitud.lote", "solicitud.lote.categoria", "solicitud.organizacion"})
    Page<Asignacion> findBySolicitud_Organizacion_IdOrganizacion(Long idOrganizacion, Pageable pageable);

    @EntityGraph(attributePaths = {"solicitud", "solicitud.lote", "solicitud.lote.categoria", "solicitud.organizacion"})
    Page<Asignacion> findBySolicitud_Lote_Donante_IdOrganizacion(Long idOrganizacion, Pageable pageable);

    @EntityGraph(attributePaths = {"solicitud", "solicitud.lote", "solicitud.lote.categoria", "solicitud.organizacion"})
    Page<Asignacion> findAllBy(Pageable pageable);
}
