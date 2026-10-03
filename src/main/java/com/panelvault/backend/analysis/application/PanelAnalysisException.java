package com.panelvault.backend.analysis.application;

/**
 * El motor de IA no pudo analizar una pagina.
 *
 * <p>{@code retryable} distingue dos casos: un fallo temporal (motor dormido, caido o lento) vale
 * la pena reintentarlo; un fallo definitivo (la imagen esta corrupta) fallaria igual siempre.
 */
public class PanelAnalysisException extends RuntimeException {

    private final boolean retryable;

    public PanelAnalysisException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public PanelAnalysisException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
