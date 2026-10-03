package com.panelvault.backend.analysis.domain;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.util.Locale;

/**
 * Configuracion de analisis del motor de IA. Cambia el sentido de lectura de las vinetas:
 * occidental (izquierda a derecha) o manga (derecha a izquierda).
 */
public enum AnalysisPreset {

    WESTERN("western"),
    MANGA("manga");

    private final String value;

    AnalysisPreset(String value) {
        this.value = value;
    }

    /** Nombre que usa la API del motor y la base de datos. */
    public String value() {
        return value;
    }

    /** Si no se indica, se asume lectura occidental. */
    public static AnalysisPreset fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return WESTERN;
        }
        String normalized = raw.strip().toLowerCase(Locale.ROOT);
        for (AnalysisPreset preset : values()) {
            if (preset.value.equals(normalized)) {
                return preset;
            }
        }
        throw new InvalidInputException(
                "analysis.unknown_preset", "Modo de lectura desconocido. Usa 'western' o 'manga'");
    }
}
