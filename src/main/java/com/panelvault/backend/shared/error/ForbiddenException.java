package com.panelvault.backend.shared.error;

/**
 * La identidad es valida pero no tiene permiso para la accion (por ejemplo, un LECTOR intentando
 * administrar usuarios). Se responde con 403.
 */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String code, String message) {
        super(ErrorCategory.FORBIDDEN, code, message);
    }
}