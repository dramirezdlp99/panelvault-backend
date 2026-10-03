package com.panelvault.backend.reading.web;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.reading.application.BookmarkSaveResult;
import com.panelvault.backend.reading.application.BookmarkService;
import com.panelvault.backend.reading.application.ReadingService;
import com.panelvault.backend.shared.error.InvalidInputException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Progreso de lectura y marcadores del usuario autenticado ({@code /api/v1/reading}). */
@RestController
@RequestMapping("/api/v1/reading")
public class ReadingController {

    private final ReadingService reading;
    private final BookmarkService bookmarks;

    public ReadingController(ReadingService reading, BookmarkService bookmarks) {
        this.reading = reading;
        this.bookmarks = bookmarks;
    }

    @PutMapping("/comics/{comicId}/progress")
    public SyncResponse sync(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String comicId, @Valid @RequestBody ProgressRequest request) {
        return SyncResponse.from(reading.sync(userOf(jwt), ComicId.parse(comicId), request.toUpdate()));
    }

    @GetMapping("/comics/{comicId}/progress")
    public ProgressResponse progress(@AuthenticationPrincipal Jwt jwt, @PathVariable String comicId) {
        return ProgressResponse.from(reading.get(userOf(jwt), ComicId.parse(comicId)));
    }

    @GetMapping("/recent")
    public List<RecentReadingResponse> recent(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(name = "limit", defaultValue = "6") int limit) {
        return reading.recent(userOf(jwt), limit).stream().map(RecentReadingResponse::from).toList();
    }

    @PutMapping("/comics/{comicId}/bookmarks/{bookmarkId}")
    public ResponseEntity<BookmarkResponse> saveBookmark(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String comicId,
            @PathVariable String bookmarkId,
            @Valid @RequestBody BookmarkRequest request) {
        BookmarkSaveResult result = bookmarks.save(
                userOf(jwt), ComicId.parse(comicId), parseBookmarkId(bookmarkId), request.page(), request.note());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(BookmarkResponse.from(result.bookmark()));
    }

    @GetMapping("/comics/{comicId}/bookmarks")
    public List<BookmarkResponse> listBookmarks(@AuthenticationPrincipal Jwt jwt, @PathVariable String comicId) {
        return bookmarks.list(userOf(jwt), ComicId.parse(comicId)).stream().map(BookmarkResponse::from).toList();
    }

    @DeleteMapping("/bookmarks/{bookmarkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBookmark(@AuthenticationPrincipal Jwt jwt, @PathVariable String bookmarkId) {
        bookmarks.delete(userOf(jwt), parseBookmarkId(bookmarkId));
    }

    private static UUID parseBookmarkId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("reading.invalid_bookmark_id", "El id del marcador no es un UUID valido");
        }
    }

    private static UserId userOf(Jwt jwt) {
        return new UserId(UUID.fromString(jwt.getSubject()));
    }
}
