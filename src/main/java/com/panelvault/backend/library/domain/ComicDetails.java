package com.panelvault.backend.library.domain;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Datos descriptivos de un comic, ya validados y normalizados (Value Object).
 *
 * <p>Las etiquetas se pasan a minusculas, sin espacios repetidos y sin duplicados, para que
 * "Spider-Man" y " spider-man " sean la misma etiqueta al filtrar.
 */
public record ComicDetails(
        String title,
        String series,
        String issueNumber,
        int pageCount,
        ComicFormat format,
        FileFingerprint fingerprint,
        ReadingDirection direction,
        Set<String> tags) {

    public static final int MAX_TITLE = 200;
    public static final int MAX_SERIES = 200;
    public static final int MAX_ISSUE = 20;
    public static final int MAX_PAGES = 2000;
    public static final int MAX_TAGS = 10;
    public static final int MAX_TAG_LENGTH = 30;

    private static final Pattern SPACES = Pattern.compile("\\s+");

    public ComicDetails {
        title = requiredText(title, MAX_TITLE, "library.invalid_title", "El titulo");
        series = optionalText(series, MAX_SERIES, "library.invalid_series", "La serie");
        issueNumber = optionalText(issueNumber, MAX_ISSUE, "library.invalid_issue", "El numero");
        if (pageCount < 1 || pageCount > MAX_PAGES) {
            throw new InvalidInputException(
                    "library.invalid_page_count", "Un comic debe tener entre 1 y " + MAX_PAGES + " paginas");
        }
        Objects.requireNonNull(format, "El formato es obligatorio");
        Objects.requireNonNull(fingerprint, "La huella es obligatoria");
        direction = direction == null ? ReadingDirection.LEFT_TO_RIGHT : direction;
        tags = normalizeTags(tags);
    }

    /** Igual que el constructor principal, pero acepta las etiquetas en cualquier coleccion (por ejemplo, una lista del JSON). */
    public ComicDetails(
            String title,
            String series,
            String issueNumber,
            int pageCount,
            ComicFormat format,
            FileFingerprint fingerprint,
            ReadingDirection direction,
            Collection<String> tags) {
        this(title, series, issueNumber, pageCount, format, fingerprint, direction, normalizeTags(tags));
    }

    private static String requiredText(String value, int max, String code, String field) {
        String clean = clean(value);
        if (clean == null) {
            throw new InvalidInputException(code, field + " es obligatorio");
        }
        if (clean.length() > max) {
            throw new InvalidInputException(code, field + " admite maximo " + max + " caracteres");
        }
        return clean;
    }

    private static String optionalText(String value, int max, String code, String field) {
        String clean = clean(value);
        if (clean != null && clean.length() > max) {
            throw new InvalidInputException(code, field + " admite maximo " + max + " caracteres");
        }
        return clean;
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return SPACES.matcher(value.strip()).replaceAll(" ");
    }

    static Set<String> normalizeTags(Collection<String> raw) {
        if (raw == null) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String tag : raw) {
            String clean = clean(tag);
            if (clean == null) {
                continue;
            }
            clean = clean.toLowerCase(Locale.ROOT);
            if (clean.length() > MAX_TAG_LENGTH || clean.contains(",")) {
                throw new InvalidInputException(
                        "library.invalid_tag", "Cada etiqueta admite maximo " + MAX_TAG_LENGTH + " caracteres y sin comas");
            }
            normalized.add(clean);
        }
        if (normalized.size() > MAX_TAGS) {
            throw new InvalidInputException("library.too_many_tags", "Un comic admite maximo " + MAX_TAGS + " etiquetas");
        }
        // Conserva el orden en que el usuario las escribio, pero sin permitir modificarlas.
        return Collections.unmodifiableSet(normalized);
    }
}
