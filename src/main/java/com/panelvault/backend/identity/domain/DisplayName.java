package com.panelvault.backend.identity.domain;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.util.regex.Pattern;

/**
 * Nombre que el usuario muestra en la aplicacion (Value Object).
 *
 * <p>Se recortan los extremos y los espacios repetidos se reducen a uno. Se rechazan caracteres de
 * control (saltos de linea, tabuladores, caracteres invisibles) porque romperian la interfaz.
 */
public record DisplayName(String value) {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 40;

    private static final Pattern REPEATED_SPACES = Pattern.compile("\\s+");
    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}|\\p{Cf}");

    public DisplayName {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException("user.display_name_invalid", "El nombre es obligatorio");
        }
        if (CONTROL_CHARS.matcher(value.strip()).find()) {
            throw new InvalidInputException(
                    "user.display_name_invalid", "El nombre contiene caracteres no permitidos");
        }
        value = REPEATED_SPACES.matcher(value.strip()).replaceAll(" ");
        int length = value.codePointCount(0, value.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new InvalidInputException(
                    "user.display_name_invalid",
                    "El nombre debe tener entre " + MIN_LENGTH + " y " + MAX_LENGTH + " caracteres");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}