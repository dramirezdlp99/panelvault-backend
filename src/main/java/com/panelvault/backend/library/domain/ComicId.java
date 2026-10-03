package com.panelvault.backend.library.domain;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.util.Objects;
import java.util.UUID;

/**
 * Identificador de un comic (Value Object).
 *
 * <p>Lo genera el NAVEGADOR, no el servidor. Es clave para el modo offline: el usuario puede
 * importar un comic sin conexion, el frontend le asigna su id y lo sincroniza despues. Si la
 * sincronizacion se reintenta, el mismo id hace que la operacion sea idempotente (no se duplica).
 */
public record ComicId(UUID value) {

    public ComicId {
        Objects.requireNonNull(value, "El id del comic es obligatorio");
    }

    public static ComicId parse(String raw) {
        try {
            return new ComicId(UUID.fromString(raw));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidInputException("library.invalid_comic_id", "El id del comic no es un UUID valido");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
