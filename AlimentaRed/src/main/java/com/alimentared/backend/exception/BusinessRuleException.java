package com.alimentared.backend.exception;

import lombok.Getter;

/** Violación de una regla de negocio (RN01..RN12). Se responde con 422. */
@Getter
public class BusinessRuleException extends RuntimeException {

    private final String codigo;

    public BusinessRuleException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public BusinessRuleException(String mensaje) {
        this("NEGOCIO", mensaje);
    }
}
