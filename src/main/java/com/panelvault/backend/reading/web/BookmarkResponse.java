package com.panelvault.backend.reading.web;

import com.panelvault.backend.reading.domain.Bookmark;
import java.time.Instant;
import java.util.UUID;

/** Un marcador. */
public record BookmarkResponse(UUID id, UUID comicId, int page, String note, Instant createdAt) {

    public static BookmarkResponse from(Bookmark b) {
        return new BookmarkResponse(b.id(), b.comic().value(), b.page(), b.note(), b.createdAt());
    }
}
