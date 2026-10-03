package com.panelvault.backend.library.domain;

import java.util.List;
import java.util.Locale;

/** Datos de ejemplo para las pruebas de biblioteca y lectura. */
public final class ComicSamples {

    private ComicSamples() {}

    /** Huella SHA-256 valida y distinta para cada semilla. */
    public static String sha256(int seed) {
        return String.format(Locale.ROOT, "%064x", seed + 1);
    }

    public static ComicDetails details(String title, int pages, int seed) {
        return new ComicDetails(title, "Amazing Fantasy", "15", pages, ComicFormat.CBZ,
                new FileFingerprint(sha256(seed)), ReadingDirection.LEFT_TO_RIGHT, List.of("Superheroes"));
    }

    public static ComicDetails details(int seed) {
        return details("Comic " + seed, 24, seed);
    }
}
