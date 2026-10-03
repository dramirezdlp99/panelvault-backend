package com.panelvault.backend.library.application;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.Comic;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.library.domain.ComicRepository;
import com.panelvault.backend.library.domain.FileFingerprint;
import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Biblioteca en memoria para pruebas (Fake), con las mismas reglas de orden y busqueda. */
public class InMemoryComicRepository implements ComicRepository {

    private final Map<ComicId, Comic> comics = new LinkedHashMap<>();

    @Override
    public Optional<Comic> findById(ComicId id) {
        return Optional.ofNullable(comics.get(id)).map(InMemoryComicRepository::copy);
    }

    @Override
    public Optional<Comic> findByOwnerAndFingerprint(UserId owner, FileFingerprint fingerprint) {
        return comics.values().stream()
                .filter(c -> c.isOwnedBy(owner) && c.details().fingerprint().equals(fingerprint))
                .findFirst()
                .map(InMemoryComicRepository::copy);
    }

    @Override
    public PageResult<Comic> findByOwner(UserId owner, String search, PageQuery page) {
        String needle = search == null ? "" : search.strip().toLowerCase(Locale.ROOT);
        List<Comic> all = comics.values().stream()
                .filter(c -> c.isOwnedBy(owner))
                .filter(c -> needle.isEmpty()
                        || c.details().title().toLowerCase(Locale.ROOT).contains(needle)
                        || (c.details().series() != null
                                && c.details().series().toLowerCase(Locale.ROOT).contains(needle)))
                .sorted(Comparator.comparing(Comic::updatedAt).reversed())
                .toList();
        List<Comic> slice = all.stream().skip(page.offset()).limit(page.size()).map(InMemoryComicRepository::copy).toList();
        return PageResult.of(slice, page, all.size());
    }

    @Override
    public Comic save(Comic comic) {
        comics.put(comic.id(), copy(comic));
        return comic;
    }

    @Override
    public void delete(ComicId id) {
        comics.remove(id);
    }

    @Override
    public long countByOwner(UserId owner) {
        return comics.values().stream().filter(c -> c.isOwnedBy(owner)).count();
    }

    @Override
    public long totalPagesByOwner(UserId owner) {
        return comics.values().stream().filter(c -> c.isOwnedBy(owner)).mapToLong(c -> c.details().pageCount()).sum();
    }

    private static Comic copy(Comic c) {
        return Comic.rehydrate(c.id(), c.owner(), c.details(), c.createdAt(), c.updatedAt(), c.version());
    }
}
