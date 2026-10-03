package com.panelvault.backend.reading.application;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.Comic;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.library.domain.ComicRepository;
import com.panelvault.backend.reading.domain.Bookmark;
import com.panelvault.backend.reading.domain.BookmarkRepository;
import com.panelvault.backend.shared.error.BusinessRuleException;
import com.panelvault.backend.shared.error.InvalidInputException;
import com.panelvault.backend.shared.error.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de los marcadores. Guardar es idempotente por id, como en la biblioteca. */
@Service
public class BookmarkService {

    public static final int MAX_PER_COMIC = 200;

    private final BookmarkRepository bookmarks;
    private final ComicRepository comics;
    private final Clock clock;

    public BookmarkService(BookmarkRepository bookmarks, ComicRepository comics, Clock clock) {
        this.bookmarks = bookmarks;
        this.comics = comics;
        this.clock = clock;
    }

    @Transactional
    public BookmarkSaveResult save(UserId user, ComicId comicId, UUID bookmarkId, int page, String note) {
        Comic comic = ownedComic(user, comicId);
        if (page > comic.details().pageCount()) {
            throw new InvalidInputException(
                    "reading.invalid_page", "La pagina debe estar entre 1 y " + comic.details().pageCount());
        }
        Optional<Bookmark> existing = bookmarks.findById(bookmarkId);
        if (existing.isPresent()) {
            Bookmark bookmark = existing
                    .filter(b -> b.isOwnedBy(user) && b.comic().equals(comicId))
                    .orElseThrow(BookmarkService::notFound);
            bookmark.change(page, note);
            return new BookmarkSaveResult(bookmarks.save(bookmark), false);
        }
        if (bookmarks.countByComic(user, comicId) >= MAX_PER_COMIC) {
            throw new BusinessRuleException(
                    "reading.bookmark_limit", "Un comic admite maximo " + MAX_PER_COMIC + " marcadores");
        }
        Bookmark created = Bookmark.create(bookmarkId, user, comicId, page, note, clock.instant());
        return new BookmarkSaveResult(bookmarks.save(created), true);
    }

    @Transactional(readOnly = true)
    public List<Bookmark> list(UserId user, ComicId comicId) {
        ownedComic(user, comicId);
        return bookmarks.findByComic(user, comicId);
    }

    @Transactional
    public void delete(UserId user, UUID bookmarkId) {
        Bookmark bookmark = bookmarks.findById(bookmarkId)
                .filter(b -> b.isOwnedBy(user))
                .orElseThrow(BookmarkService::notFound);
        bookmarks.delete(bookmark.id());
    }

    private Comic ownedComic(UserId user, ComicId comicId) {
        return comics.findById(comicId)
                .filter(c -> c.isOwnedBy(user))
                .orElseThrow(() -> new NotFoundException("library.comic_not_found", "No existe ese comic en tu biblioteca"));
    }

    private static NotFoundException notFound() {
        return new NotFoundException("reading.bookmark_not_found", "No existe ese marcador");
    }
}
