package com.alimentared.backend.repository.specification;

import com.alimentared.backend.entity.Lote;
import com.alimentared.backend.entity.enums.EstadoLote;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Filtros combinables del catálogo de lotes (RF04). Un filtro nulo no restringe. */
public final class LoteSpecifications {

    private LoteSpecifications() {
    }

    /** Carga categoría y donante en la misma consulta (no aplica a la consulta de conteo). */
    public static Specification<Lote> conRelaciones() {
        return (root, query, cb) -> {
            if (query != null && !SpecificationUtils.esConsultaDeConteo(query)) {
                root.fetch("categoria", JoinType.INNER);
                root.fetch("donante", JoinType.INNER);
            }
            return null;
        };
    }

    public static Specification<Lote> categoria(Long idCategoria) {
        return idCategoria == null ? null
                : (root, query, cb) -> cb.equal(root.get("categoria").get("idCategoria"), idCategoria);
    }

    public static Specification<Lote> ubicacionContiene(String texto) {
        return texto == null || texto.isBlank() ? null
                : (root, query, cb) -> cb.like(cb.lower(root.get("ubicacion")), "%" + texto.trim().toLowerCase() + "%");
    }

    public static Specification<Lote> estado(EstadoLote estado) {
        return estado == null ? null : (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Lote> estadoDistintoDe(EstadoLote estado) {
        return (root, query, cb) -> cb.notEqual(root.get("estado"), estado);
    }

    public static Specification<Lote> caducaDesde(LocalDateTime desde) {
        return desde == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaCaducidad"), desde);
    }

    public static Specification<Lote> caducaAntesDe(LocalDateTime hasta) {
        return hasta == null ? null : (root, query, cb) -> cb.lessThan(root.get("fechaCaducidad"), hasta);
    }

    public static Specification<Lote> noVencido(LocalDateTime ahora) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaCaducidad"), ahora);
    }

    public static Specification<Lote> conInventario() {
        return (root, query, cb) -> cb.greaterThan(root.get("cantidadDisponible"), BigDecimal.ZERO);
    }

    public static Specification<Lote> registradoEntre(LocalDateTime inicio, LocalDateTime fin) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("fechaRegistro"), inicio),
                cb.lessThan(root.get("fechaRegistro"), fin));
    }
}
