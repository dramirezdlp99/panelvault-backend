package com.panelvault.backend.identity.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador de un usuario (Value Object).
 *
 * <p>Se genera en el dominio y no en la base de datos: asi el usuario tiene identidad desde que
 * nace, antes de guardarse, y un id aleatorio no revela cuantas cuentas existen (un autoincremental
 * si lo haria).
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "El id del usuario es obligatorio");
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}