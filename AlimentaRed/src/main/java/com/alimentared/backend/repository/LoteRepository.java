package com.alimentared.backend.repository;

import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.enums.EstadoLote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long>, JpaSpecificationExecutor<Lote> {

    @EntityGraph(attributePaths = {"categoria", "donante"})
    Page<Lote> findByDonante_IdOrganizacion(Long idOrganizacion, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "donante"})
    Page<Lote> findByDonante_IdOrganizacionAndEstado(Long idOrganizacion, EstadoLote estado, Pageable pageable);

    List<Lote> findByEstadoInAndFechaCaducidadBefore(Collection<EstadoLote> estados, LocalDateTime fecha);

    boolean existsByCategoria_IdCategoria(Long idCategoria);
}
