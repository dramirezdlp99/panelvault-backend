package com.panelvault.backend.reading.application;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.reading.domain.Bookmark;
import com.panelvault.backend.reading.domain.BookmarkRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Marcadores en memoria para pruebas (Fake). */
public class InMemoryBookmarkRepository implements BookmarkRepository {

    private final Map<UUID, Bookmark> rows = new LinkedHashMap<>();

    @Override
    public Optional<Bookmark> findById(UUID id) {
        return Optional.ofNullable(rows.get(id)).map(InMemoryBookmarkRepository::copy);
    }

    @Override
    public List<Bookmark> findByComic(UserId user, ComicId comic) {
        return rows.values().stream()
                .filter(b -> b.user().equals(user) && b.comic().equals(comic))
                .sorted(Comparator.comparingInt(Bookmark::page).thenComparing(Bookmark::createdAt))
                .map(InMemoryBookmarkRepository::copy)
                .toList();
    }

    @Override
    public long countByComic(UserId user, ComicId comic) {
        return rows.values().stream().filter(b -> b.user().equals(user) && b.comic().equals(comic)).count();
    }

    @Override
    public Bookmark save(Bookmark bookmark) {
        rows.put(bookmark.id(), copy(bookmark));
        return bookmark;
    }

    @Override
    public void delete(UUID id) {
        rows.remove(id);
    }

    private static Bookmark copy(Bookmark b) {
        return Bookmark.rehydrate(b.id(), b.user(), b.comic(), b.page(), b.note(), b.createdAt());
    }
}
