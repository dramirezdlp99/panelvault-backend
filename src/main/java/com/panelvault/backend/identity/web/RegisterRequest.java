package com.panelvault.backend.identity.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo JSON de {@code POST /api/v1/auth/register}.
 *
 * <p>Esta validacion es la primera barrera (rechaza basura obvia con un 400 y la lista de campos).
 * Las reglas de verdad viven en el dominio; aqui solo se ponen topes para no procesar entradas
 * absurdas, como una contrasena de un megabyte.
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 40) String displayName,
        @NotBlank @Size(max = 128) String password) {

    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", displayName=" + displayName + ", password=***]";
    }
}