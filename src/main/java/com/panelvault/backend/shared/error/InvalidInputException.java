package com.panelvault.backend.shared.error;

/**
 * Un dato de entrada viola una regla del dominio (formato de correo, politica de contrasena...).
 *
 * <p>Se diferencia de la validacion de Bean Validation en el controlador: aquella protege el borde
 * HTTP; esta protege el dominio, sin importar quien lo invoque (API, prueba, tarea programada).
 */
public class InvalidInputException extends DomainException {

    public InvalidInputException(String code, String message) {
        super(ErrorCategory.INVALID_INPUT, code, message);
    }
}