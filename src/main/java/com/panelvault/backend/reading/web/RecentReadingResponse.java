package com.panelvault.backend.reading.web;

import com.panelvault.backend.reading.application.RecentReading;
import java.util.UUID;

/** Un elemento de "continuar leyendo" en el panel principal. */
public record RecentReadingResponse(
        UUID comicId, String title, String series, String issueNumber, ProgressResponse progress) {

    public static RecentReadingResponse from(RecentReading recent) {
        var details = recent.comic().details();
        return new RecentReadingResponse(
                recent.comic().id().value(),
                details.title(),
                details.series(),
                details.issueNumber(),
                ProgressResponse.from(recent.progress()));
    }
}
