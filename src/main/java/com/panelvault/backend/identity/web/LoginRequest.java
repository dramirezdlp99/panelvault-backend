package com.panelvault.backend.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo JSON de {@code POST /api/v1/auth/login}.
 *
 * <p>Aqui no se valida el formato del correo: cualquier dato incorrecto debe terminar en el mismo
 * "correo o contrasena incorrectos", sin pistas.
 */
public record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 128) String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}