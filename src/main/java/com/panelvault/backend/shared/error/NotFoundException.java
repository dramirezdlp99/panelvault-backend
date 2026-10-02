package com.panelvault.backend.shared.error;

/** El recurso solicitado no existe (o el usuario no tiene forma de saber que existe). */
public class NotFoundException extends DomainException {

    public NotFoundException(String code, String message) {
        super(ErrorCategory.NOT_FOUND, code, message);
    }
}