package com.panelvault.backend.shared.error;

import java.util.Objects;

/**
 * Base de todas las excepciones de negocio del sistema.
 *
 * <p>Cada una lleva una categoria y un codigo estable para maquinas, por ejemplo
 * {@code "user.email_taken"}. El frontend decide que hacer segun el codigo, nunca segun el texto
 * del mensaje, que puede cambiar.
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorCategory category;
    private final String code;

    protected DomainException(ErrorCategory category, String code, String message) {
        super(message);
        this.category = Objects.requireNonNull(category, "La categoria del error es obligatoria");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El codigo del error es obligatorio");
        }
        this.code = code;
    }

    public ErrorCategory category() {
        return category;
    }

    public String code() {
        return code;
    }
}