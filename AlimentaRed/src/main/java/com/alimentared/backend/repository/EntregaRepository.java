package com.alimentared.backend.repository;

import com.alimentared.backend.entity.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long>, JpaSpecificationExecutor<Entrega> {

    List<Entrega> findByAsignacion_IdAsignacionOrderByFechaEntregaAsc(Long idAsignacion);
}
