package com.panelvault.backend.identity.domain;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Correo electronico normalizado (Value Object).
 *
 * <p>Se guarda sin espacios y en minusculas, de modo que "Ana@Mail.com" y "ana@mail.com" sean la
 * misma cuenta. La base de datos refuerza la misma regla con un CHECK, por si alguien escribe en la
 * tabla sin pasar por aqui.
 */
public record Email(String value) {

    /** Longitud maxima de una direccion segun RFC 5321. */
    public static final int MAX_LENGTH = 254;

    private static final Pattern FORMAT =
            Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException("user.email_invalid", "El correo es obligatorio");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new InvalidInputException("user.email_invalid", "El correo no tiene un formato valido");
        }
    }

    /** Parte anterior a la arroba. La politica de contrasenas la usa para evitar claves obvias. */
    public String localPart() {
        return value.substring(0, value.indexOf('@'));
    }

    @Override
    public String toString() {
        return value;
    }
}