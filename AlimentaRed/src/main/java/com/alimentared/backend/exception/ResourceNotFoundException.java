package com.alimentared.backend.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }

    public ResourceNotFoundException(String recurso, Object id) {
        super("%s con id %s no encontrado".formatted(recurso, id));
    }
}
