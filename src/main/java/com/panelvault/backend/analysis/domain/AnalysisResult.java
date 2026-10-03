package com.panelvault.backend.analysis.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Resultado del motor de IA para una pagina y un modo de lectura: el mapa de vinetas con su orden.
 *
 * <p>{@code payloadJson} es la respuesta del motor tal cual (JSON). El backend no necesita
 * interpretarla: la guarda en la cache y se la entrega al lector del frontend.
 */
public record AnalysisResult(PageHash pageHash, AnalysisPreset preset, String payloadJson, Instant createdAt) {

    public AnalysisResult {
        Objects.requireNonNull(pageHash);
        Objects.requireNonNull(preset);
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException("El resultado no puede estar vacio");
        }
        createdAt = Objects.requireNonNull(createdAt).truncatedTo(ChronoUnit.MICROS);
    }
}
