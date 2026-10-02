package com.panelvault.backend.identity.application;

/**
 * Datos de entrada para registrar una cuenta (Command).
 *
 * <p>Viene sin validar: las reglas se aplican en el dominio. El toString oculta la contrasena
 * porque un record la imprimiria tal cual en cualquier log.
 */
public record RegisterUserCommand(String email, String displayName, String password) {

    @Override
    public String toString() {
        return "RegisterUserCommand[email=" + email + ", displayName=" + displayName + ", password=***]";
    }
}