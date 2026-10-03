package com.panelvault.backend.catalog.domain;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Identificador legible de una obra en la URL, por ejemplo {@code /explorar/little-nemo-in-slumberland}
 * (Value Object).
 *
 * <p>Es estable: no cambia aunque se corrija el titulo. Las paginas del catalogo se generan de forma
 * estatica e incremental (ISR) a partir de estos slugs, y un enlace compartido debe seguir sirviendo.
 */
public record WorkSlug(String value) {

    public static final int MAX_LENGTH = 80;
    private static final Pattern VALID = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    public WorkSlug {
        if (value == null || value.length() > MAX_LENGTH || !VALID.matcher(value).matches()) {
            throw new InvalidInputException(
                    "catalog.invalid_slug", "El slug solo admite minusculas, numeros y guiones (maximo " + MAX_LENGTH + ")");
        }
    }

    /**
     * Convierte un titulo en slug:
     * <ol>
     *   <li>Normalizacion Unicode NFD: separa cada letra de su tilde ("e" + "´").</li>
     *   <li>Se eliminan las marcas diacriticas: "Ñandú" queda "Nandu".</li>
     *   <li>Minusculas, y cualquier tramo que no sea letra o numero se vuelve un guion.</li>
     *   <li>Se recortan guiones en los extremos y se limita el largo sin cortar a mitad de palabra.</li>
     * </ol>
     */
    public static WorkSlug fromTitle(String title) {
        String text = title == null ? "" : title;
        String withoutMarks = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        String slug = NON_ALPHANUMERIC.matcher(withoutMarks.toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = trimHyphens(slug);
        if (slug.length() > MAX_LENGTH) {
            int cut = slug.lastIndexOf('-', MAX_LENGTH);
            slug = trimHyphens(slug.substring(0, cut > 0 ? cut : MAX_LENGTH));
        }
        return new WorkSlug(slug.isEmpty() ? "obra" : slug);
    }

    /** Variante numerada para resolver choques: {@code little-nemo-2}. */
    public WorkSlug withSuffix(int number) {
        String suffix = "-" + number;
        String base = value.length() + suffix.length() > MAX_LENGTH
                ? trimHyphens(value.substring(0, MAX_LENGTH - suffix.length()))
                : value;
        return new WorkSlug(base + suffix);
    }

    private static String trimHyphens(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && text.charAt(start) == '-') {
            start++;
        }
        while (end > start && text.charAt(end - 1) == '-') {
            end--;
        }
        return text.substring(start, end);
    }

    @Override
    public String toString() {
        return value;
    }
}
