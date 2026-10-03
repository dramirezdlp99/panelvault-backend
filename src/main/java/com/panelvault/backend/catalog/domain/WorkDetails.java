package com.panelvault.backend.catalog.domain;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Year;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Datos de una obra del catalogo publico, validados y normalizados (Value Object).
 *
 * <p>Las URLs deben ser HTTPS con dominio: el frontend las muestra como enlaces e imagenes, y una
 * URL {@code javascript:} o {@code http:} seria un riesgo (XSS o contenido mixto).
 */
public record WorkDetails(
        String title,
        String author,
        Integer year,
        String publisher,
        String description,
        String sourceUrl,
        String coverUrl,
        License license,
        Integer pageCount,
        Set<String> tags) {

    public static final int MAX_TITLE = 200;
    public static final int MAX_AUTHOR = 120;
    public static final int MAX_PUBLISHER = 120;
    public static final int MAX_DESCRIPTION = 2000;
    public static final int MAX_URL = 500;
    public static final int MIN_YEAR = 1800;
    public static final int MAX_TAGS = 10;

    private static final Pattern SPACES = Pattern.compile("[ \\t]+");

    public WorkDetails {
        title = required(title, MAX_TITLE, "catalog.invalid_title", "El titulo");
        author = required(author, MAX_AUTHOR, "catalog.invalid_author", "El autor");
        publisher = optional(publisher, MAX_PUBLISHER, "catalog.invalid_publisher", "La editorial");
        description = optional(description, MAX_DESCRIPTION, "catalog.invalid_description", "La descripcion");
        sourceUrl = httpsUrl(sourceUrl, true, "catalog.invalid_source_url", "La fuente");
        coverUrl = httpsUrl(coverUrl, false, "catalog.invalid_cover_url", "La portada");
        Objects.requireNonNull(license, "La licencia es obligatoria");
        int currentYear = Year.now().getValue();
        if (year != null && (year < MIN_YEAR || year > currentYear)) {
            throw new InvalidInputException(
                    "catalog.invalid_year", "El ano debe estar entre " + MIN_YEAR + " y " + currentYear);
        }
        if (pageCount != null && (pageCount < 1 || pageCount > 5000)) {
            throw new InvalidInputException("catalog.invalid_page_count", "Las paginas deben estar entre 1 y 5000");
        }
        tags = normalizeTags(tags);
    }

    /** Igual que el constructor principal, pero acepta las etiquetas en cualquier coleccion. */
    public WorkDetails(
            String title,
            String author,
            Integer year,
            String publisher,
            String description,
            String sourceUrl,
            String coverUrl,
            License license,
            Integer pageCount,
            Collection<String> tags) {
        this(title, author, year, publisher, description, sourceUrl, coverUrl, license, pageCount, normalizeTags(tags));
    }

    private static String required(String value, int max, String code, String field) {
        String clean = clean(value);
        if (clean == null) {
            throw new InvalidInputException(code, field + " es obligatorio");
        }
        return checkLength(clean, max, code, field);
    }

    private static String optional(String value, int max, String code, String field) {
        String clean = clean(value);
        return clean == null ? null : checkLength(clean, max, code, field);
    }

    private static String checkLength(String value, int max, String code, String field) {
        if (value.length() > max) {
            throw new InvalidInputException(code, field + " admite maximo " + max + " caracteres");
        }
        return value;
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return SPACES.matcher(value.strip()).replaceAll(" ");
    }

    /** Valida que sea una URL absoluta HTTPS con dominio. */
    static String httpsUrl(String value, boolean required, String code, String field) {
        String clean = clean(value);
        if (clean == null) {
            if (required) {
                throw new InvalidInputException(code, field + " es obligatoria");
            }
            return null;
        }
        if (clean.length() > MAX_URL) {
            throw new InvalidInputException(code, field + " admite maximo " + MAX_URL + " caracteres");
        }
        try {
            URI uri = new URI(clean);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new InvalidInputException(code, field + " debe ser una URL https valida");
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new InvalidInputException(code, field + " debe ser una URL https valida");
        }
    }

    private static Set<String> normalizeTags(Collection<String> raw) {
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
            if (clean.length() > 30 || clean.contains(",")) {
                throw new InvalidInputException("catalog.invalid_tag", "Cada etiqueta admite maximo 30 caracteres y sin comas");
            }
            normalized.add(clean);
        }
        if (normalized.size() > MAX_TAGS) {
            throw new InvalidInputException("catalog.too_many_tags", "Una obra admite maximo " + MAX_TAGS + " etiquetas");
        }
        return Collections.unmodifiableSet(normalized);
    }
}
