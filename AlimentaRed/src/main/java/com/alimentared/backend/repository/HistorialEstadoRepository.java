package com.alimentared.backend.repository;

import com.alimentared.backend.entity.HistorialEstado;
import com.alimentared.backend.entity.enums.EntidadAuditada;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    @EntityGraph(attributePaths = "usuarioResponsable")
    List<HistorialEstado> findByEntidadAndEntidadIdOrderByFechaAscIdHistorialAsc(EntidadAuditada entidad, Long entidadId);
}
