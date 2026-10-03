package com.panelvault.backend.reading.domain;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.ComicId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida de los marcadores. */
public interface BookmarkRepository {

    Optional<Bookmark> findById(UUID id);

    /** Marcadores de un comic, ordenados por pagina. */
    List<Bookmark> findByComic(UserId user, ComicId comic);

    long countByComic(UserId user, ComicId comic);

    Bookmark save(Bookmark bookmark);

    void delete(UUID id);
}
