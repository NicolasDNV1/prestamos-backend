package com.alimentared.backend.repository;

import com.alimentared.backend.entity.Incidencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    @EntityGraph(attributePaths = {"lote", "usuarioReporta"})
    Page<Incidencia> findByResuelta(boolean resuelta, Pageable pageable);

    @EntityGraph(attributePaths = {"lote", "usuarioReporta"})
    List<Incidencia> findByLote_IdLoteOrderByFechaReporteDesc(Long idLote);

    boolean existsByLote_IdLoteAndResueltaFalseAndIdIncidenciaNot(Long idLote, Long idIncidencia);
}
