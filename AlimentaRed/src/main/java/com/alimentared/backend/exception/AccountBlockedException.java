package com.alimentared.backend.exception;

/** RN08: cuenta suspendida o inactiva. */
public class AccountBlockedException extends RuntimeException {

    public AccountBlockedException(String mensaje) {
        super(mensaje);
    }
}
