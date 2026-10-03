package com.panelvault.backend.reading.web;

import com.panelvault.backend.reading.application.SyncResult;

/**
 * Respuesta de sincronizar el progreso. Si {@code applied} es falso, otro dispositivo guardo un
 * progreso mas reciente y el cliente debe mostrar {@code progress}.
 */
public record SyncResponse(boolean applied, ProgressResponse progress) {

    public static SyncResponse from(SyncResult result) {
        return new SyncResponse(result.applied(), ProgressResponse.from(result.progress()));
    }
}
