package com.panelvault.backend.identity.domain;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Politica de contrasenas (Specification).
 *
 * <p>Reglas, cada una con su razon:
 * <ul>
 *   <li>Minimo 10 caracteres: la longitud aporta mas seguridad que los simbolos raros.</li>
 *   <li>Maximo 72 bytes en UTF-8: BCrypt solo procesa los primeros 72 bytes. Una clave mas larga
 *       se truncaria en silencio y dos claves distintas podrian dar el mismo hash.</li>
 *   <li>Al menos una letra y un digito.</li>
 *   <li>No puede contener el usuario del correo (lo anterior a la arroba).</li>
 * </ul>
 *
 * <p>Se reportan todas las fallas juntas para que el usuario corrija de una vez.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_BYTES = 72;

    /** Partes del correo mas cortas que esto no se buscan: "ana" daria demasiados falsos positivos. */
    private static final int MIN_LOCAL_PART_TO_CHECK = 4;

    /** Lanza {@link InvalidInputException} con codigo {@code user.weak_password} si hay fallas. */
    public void check(String rawPassword, Email email) {
        List<String> problems = violations(rawPassword, email);
        if (!problems.isEmpty()) {
            throw new InvalidInputException(
                    "user.weak_password",
                    "La contrasena no cumple la politica: " + String.join("; ", problems));
        }
    }

    /** Lista de reglas incumplidas; vacia si la contrasena es aceptable. */
    public List<String> violations(String rawPassword, Email email) {
        List<String> problems = new ArrayList<>();
        if (rawPassword == null || rawPassword.isBlank()) {
            problems.add("es obligatoria");
            return problems;
        }
        if (rawPassword.codePointCount(0, rawPassword.length()) < MIN_LENGTH) {
            problems.add("debe tener al menos " + MIN_LENGTH + " caracteres");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            problems.add("es demasiado larga (maximo " + MAX_BYTES + " bytes)");
        }
        if (rawPassword.codePoints().noneMatch(Character::isLetter)) {
            problems.add("debe incluir al menos una letra");
        }
        if (rawPassword.codePoints().noneMatch(Character::isDigit)) {
            problems.add("debe incluir al menos un digito");
        }
        if (email != null) {
            String localPart = email.localPart();
            if (localPart.length() >= MIN_LOCAL_PART_TO_CHECK
                    && rawPassword.toLowerCase(Locale.ROOT).contains(localPart)) {
                problems.add("no puede contener tu usuario de correo");
            }
        }
        return problems;
    }
}