package com.panelvault.backend.shared.error;

/** La operacion choca con el estado actual, por ejemplo un correo que ya esta registrado. */
public class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(ErrorCategory.CONFLICT, code, message);
    }
}