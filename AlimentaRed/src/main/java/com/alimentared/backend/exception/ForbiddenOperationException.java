package com.alimentared.backend.exception;

/** El usuario está autenticado pero no es responsable del recurso. */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String mensaje) {
        super(mensaje);
    }
}
