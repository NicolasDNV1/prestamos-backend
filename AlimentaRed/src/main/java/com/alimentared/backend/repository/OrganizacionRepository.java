package com.alimentared.backend.repository;

import com.alimentared.backend.entity.Organizacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizacionRepository extends JpaRepository<Organizacion, Long> {

    /** Serializa operaciones concurrentes de una misma organización (RN07). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Organizacion o where o.idOrganizacion = :id")
    Optional<Organizacion> findByIdForUpdate(@Param("id") Long id);

    boolean existsByNit(String nit);

    boolean existsByNitAndIdOrganizacionNot(String nit, Long idOrganizacion);

    Page<Organizacion> findByVerificada(boolean verificada, Pageable pageable);

    long countByBloqueadaTrue();

    List<Organizacion> findByStrikesVigentesGreaterThanOrderByStrikesVigentesDesc(int strikes);
}
