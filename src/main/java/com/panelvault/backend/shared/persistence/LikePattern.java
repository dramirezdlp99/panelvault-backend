package com.panelvault.backend.shared.persistence;

import java.util.Locale;

/**
 * Construye patrones LIKE seguros para busquedas de texto.
 *
 * <p>En SQL, {@code %} y {@code _} son comodines. Si el usuario busca "100%" sin escaparlo, la
 * busqueda haria algo distinto a lo que pidio. Se escapan con {@code \} y la consulta debe declarar
 * {@code ESCAPE '\'}.
 */
public final class LikePattern {

    private LikePattern() {}

    /** Patron "contiene", en minusculas, para comparar con {@code lower(columna)}. */
    public static String contains(String search) {
        String escaped = search.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
