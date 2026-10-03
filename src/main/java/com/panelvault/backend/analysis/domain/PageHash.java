package com.panelvault.backend.analysis.domain;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Huella SHA-256 de una pagina (Value Object): 64 caracteres hexadecimales en minuscula.
 *
 * <p>Es la clave de la cache de analisis. Dos personas que suben exactamente la misma pagina (por
 * ejemplo, el mismo comic de dominio publico) producen la misma huella, asi que el motor de IA la
 * procesa una sola vez y el segundo usuario recibe el resultado al instante.
 */
public record PageHash(String value) {

    private static final Pattern HEX_64 = Pattern.compile("[0-9a-f]{64}");

    public PageHash {
        value = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        if (!HEX_64.matcher(value).matches()) {
            throw new InvalidInputException(
                    "analysis.invalid_page_hash", "La huella de la pagina debe ser un SHA-256 en hexadecimal");
        }
    }

    /** Calcula la huella de los bytes de una imagen. */
    public static PageHash of(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return new PageHash(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible en esta JVM", e);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
