package com.alimentared.backend.repository.specification;

import jakarta.persistence.criteria.CriteriaQuery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Objects;

public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    /** Combina con AND ignorando los filtros nulos. */
    @SafeVarargs
    public static <T> Specification<T> todas(Specification<T>... specs) {
        return Arrays.stream(specs)
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> null);
    }

    static boolean esConsultaDeConteo(CriteriaQuery<?> query) {
        Class<?> tipo = query.getResultType();
        return Long.class.equals(tipo) || long.class.equals(tipo);
    }
}
