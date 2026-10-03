package com.panelvault.backend.library.web;

import com.panelvault.backend.library.domain.Comic;
import com.panelvault.backend.library.domain.ComicDetails;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Representacion de un comic de la biblioteca. */
public record ComicResponse(
        UUID id,
        String title,
        String series,
        String issueNumber,
        int pageCount,
        String format,
        String fileSha256,
        String readingDirection,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public static ComicResponse from(Comic comic) {
        ComicDetails d = comic.details();
        return new ComicResponse(
                comic.id().value(),
                d.title(),
                d.series(),
                d.issueNumber(),
                d.pageCount(),
                d.format().name(),
                d.fingerprint().value(),
                d.direction().name(),
                List.copyOf(d.tags()),
                comic.createdAt(),
                comic.updatedAt(),
                comic.version());
    }
}
