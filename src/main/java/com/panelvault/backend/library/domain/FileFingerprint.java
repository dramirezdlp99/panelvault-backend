package com.panelvault.backend.library.domain;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Huella SHA-256 del archivo completo del comic, calculada en el navegador.
 *
 * <p>Sirve para detectar que el usuario ya importo ese mismo archivo antes (aunque le haya cambiado
 * el nombre) y no duplicarlo en su biblioteca.
 */
public record FileFingerprint(String value) {

    private static final Pattern HEX_64 = Pattern.compile("[0-9a-f]{64}");

    public FileFingerprint {
        value = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        if (!HEX_64.matcher(value).matches()) {
            throw new InvalidInputException(
                    "library.invalid_fingerprint", "La huella del archivo debe ser un SHA-256 en hexadecimal");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
