package com.alimentared.backend.repository.specification;

import com.alimentared.backend.entity.Asignacion;
import com.alimentared.backend.entity.Entrega;
import com.alimentared.backend.entity.Solicitud;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/** Filtros por periodo, categoría y organización para historial e indicadores (RF09). */
public final class ReporteSpecifications {

    private ReporteSpecifications() {
    }

    public static Specification<Entrega> entregasConRelaciones() {
        return (root, query, cb) -> {
            if (query != null && !SpecificationUtils.esConsultaDeConteo(query)) {
                Fetch<Object, Object> solicitud = root.fetch("solicitud", JoinType.INNER);
                Fetch<Object, Object> lote = solicitud.fetch("lote", JoinType.INNER);
                lote.fetch("categoria", JoinType.INNER);
                lote.fetch("donante", JoinType.INNER);
                solicitud.fetch("organizacion", JoinType.INNER);
                root.fetch("asignacion", JoinType.INNER);
                root.fetch("usuarioResponsable", JoinType.INNER);
            }
            return null;
        };
    }

    public static Specification<Entrega> entregadasEntre(LocalDateTime inicio, LocalDateTime fin) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("fechaEntrega"), inicio),
                cb.lessThan(root.get("fechaEntrega"), fin));
    }

    public static Specification<Entrega> entregasDeCategoria(Long idCategoria) {
        return idCategoria == null ? null : (root, query, cb) -> cb.equal(
                root.get("solicitud").get("lote").get("categoria").get("idCategoria"), idCategoria);
    }

    public static Specification<Entrega> entregasDeDonante(Long idOrganizacion) {
        return (root, query, cb) -> cb.equal(
                root.get("solicitud").get("lote").get("donante").get("idOrganizacion"), idOrganizacion);
    }

    public static Specification<Entrega> entregasDeReceptora(Long idOrganizacion) {
        return (root, query, cb) -> cb.equal(
                root.get("solicitud").get("organizacion").get("idOrganizacion"), idOrganizacion);
    }

    public static Specification<Solicitud> solicitudesCreadasEntre(LocalDateTime inicio, LocalDateTime fin) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("fechaCreacion"), inicio),
                cb.lessThan(root.get("fechaCreacion"), fin));
    }

    public static Specification<Solicitud> solicitudesDeCategoria(Long idCategoria) {
        return idCategoria == null ? null : (root, query, cb) -> cb.equal(
                root.get("lote").get("categoria").get("idCategoria"), idCategoria);
    }

    public static Specification<Asignacion> asignacionesEntre(LocalDateTime inicio, LocalDateTime fin) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("fechaAsignacion"), inicio),
                cb.lessThan(root.get("fechaAsignacion"), fin));
    }

    public static Specification<Asignacion> asignacionesDeCategoria(Long idCategoria) {
        return idCategoria == null ? null : (root, query, cb) -> cb.equal(
                root.get("solicitud").get("lote").get("categoria").get("idCategoria"), idCategoria);
    }
}
