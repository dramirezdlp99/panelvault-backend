package com.panelvault.backend.shared.error;

/**
 * La peticion no trae una identidad valida: credenciales incorrectas, token ausente, vencido o
 * reutilizado. Se responde con 401.
 *
 * <p>Los mensajes de esta excepcion deben ser genericos a proposito: decir "el correo no existe" o
 * "la contrasena es incorrecta" por separado le permitiria a un atacante descubrir que cuentas
 * existen.
 */
public class UnauthenticatedException extends DomainException {

    public UnauthenticatedException(String code, String message) {
        super(ErrorCategory.UNAUTHENTICATED, code, message);
    }
}