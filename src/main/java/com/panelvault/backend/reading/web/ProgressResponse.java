package com.panelvault.backend.reading.web;

import com.panelvault.backend.reading.domain.ReadingProgress;
import java.time.Instant;
import java.util.UUID;

/** Progreso de lectura de un comic. */
public record ProgressResponse(
        UUID comicId,
        int currentPage,
        int totalPages,
        int currentPanel,
        boolean guidedMode,
        boolean finished,
        double percent,
        Instant clientUpdatedAt,
        String deviceId,
        Instant serverUpdatedAt) {

    public static ProgressResponse from(ReadingProgress p) {
        return new ProgressResponse(
                p.comic().value(),
                p.currentPage(),
                p.totalPages(),
                p.currentPanel(),
                p.guidedMode(),
                p.isFinished(),
                p.percent(),
                p.clientUpdatedAt(),
                p.deviceId(),
                p.serverUpdatedAt());
    }
}
