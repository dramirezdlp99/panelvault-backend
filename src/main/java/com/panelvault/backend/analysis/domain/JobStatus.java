package com.panelvault.backend.analysis.domain;

/**
 * Estados de un trabajo de analisis.
 *
 * <pre>
 *   PENDING --(un worker lo toma)--> RUNNING --(exito)--> SUCCEEDED
 *      ^                                |
 *      +---(fallo temporal, reintento)--+--(fallo definitivo o sin reintentos)--> FAILED
 * </pre>
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED;

    public boolean isFinished() {
        return this == SUCCEEDED || this == FAILED;
    }
}
