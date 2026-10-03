package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.AnalysisPreset;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Motor de IA falso y programable: por defecto responde un mapa de vinetas valido; se le pueden
 * encolar fallos para simular un motor dormido, caido o una imagen corrupta.
 */
public class FakePanelAnalyzer implements PanelAnalyzer {

    public static final String RESPONSE = "{\"engineVersion\":\"test\",\"panelMap\":{\"panels\":[]}}";

    private final Deque<PanelAnalysisException> failures = new ArrayDeque<>();
    private PanelAnalysisException availabilityFailure;
    private int calls;

    public void failNext(String message, boolean retryable) {
        failures.add(new PanelAnalysisException(message, retryable));
    }

    public void beUnavailable(String message) {
        availabilityFailure = new PanelAnalysisException(message, true);
    }

    @Override
    public void ensureAvailable() {
        if (availabilityFailure != null) {
            throw availabilityFailure;
        }
    }

    @Override
    public String analyze(byte[] image, AnalysisPreset preset) {
        calls++;
        if (!failures.isEmpty()) {
            throw failures.poll();
        }
        return RESPONSE;
    }

    public int calls() {
        return calls;
    }
}
