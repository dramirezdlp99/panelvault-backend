package com.panelvault.backend.library.application;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.Comic;
import com.panelvault.backend.library.domain.ComicDetails;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.library.domain.ComicRepository;
import com.panelvault.backend.library.domain.LibraryStats;
import com.panelvault.backend.shared.error.BusinessRuleException;
import com.panelvault.backend.shared.error.ConflictException;
import com.panelvault.backend.shared.error.NotFoundException;
import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de la biblioteca de un usuario.
 *
 * <p>Guardar es un "upsert" idempotente por id: el frontend puede reintentar la sincronizacion
 * cuantas veces haga falta (por ejemplo, al recuperar la conexion) sin crear duplicados.
 *
 * <p>Un comic de otro usuario se responde como 404, igual que uno inexistente: asi no se puede
 * averiguar que ids existen probando.
 */
@Service
public class LibraryService {

    /** Tope de comics por usuario, para proteger la base de datos gratuita. */
    public static final int MAX_COMICS_PER_USER = 5000;

    private final ComicRepository comics;
    private final ReadingStatsProvider readingStats;
    private final Clock clock;

    public LibraryService(ComicRepository comics, ReadingStatsProvider readingStats, Clock clock) {
        this.comics = comics;
        this.readingStats = readingStats;
        this.clock = clock;
    }

    @Transactional
    public UpsertResult save(UserId owner, ComicId id, ComicDetails details) {
        Optional<Comic> existing = comics.findById(id);
        if (existing.isPresent()) {
            Comic comic = existing.filter(c -> c.isOwnedBy(owner)).orElseThrow(LibraryService::notFound);
            rejectDuplicate(owner, details, id);
            if (comic.update(details, clock.instant())) {
                comics.save(comic);
            }
            return new UpsertResult(comic, false);
        }
        rejectDuplicate(owner, details, id);
        if (comics.countByOwner(owner) >= MAX_COMICS_PER_USER) {
            throw new BusinessRuleException(
                    "library.limit_reached", "Tu biblioteca llego al maximo de " + MAX_COMICS_PER_USER + " comics");
        }
        return new UpsertResult(comics.save(Comic.add(id, owner, details, clock.instant())), true);
    }

    @Transactional(readOnly = true)
    public Comic get(UserId owner, ComicId id) {
        return comics.findById(id).filter(c -> c.isOwnedBy(owner)).orElseThrow(LibraryService::notFound);
    }

    @Transactional(readOnly = true)
    public PageResult<Comic> list(UserId owner, String search, PageQuery page) {
        return comics.findByOwner(owner, search, page);
    }

    /** Quita el comic de la biblioteca. Su progreso y marcadores se borran con el (en cascada). */
    @Transactional
    public void delete(UserId owner, ComicId id) {
        get(owner, id);
        comics.delete(id);
    }

    @Transactional(readOnly = true)
    public LibraryStats stats(UserId owner) {
        return new LibraryStats(
                comics.countByOwner(owner),
                comics.totalPagesByOwner(owner),
                readingStats.comicsStarted(owner),
                readingStats.comicsFinished(owner));
    }

    /** El mismo archivo no puede estar dos veces en la biblioteca del mismo usuario. */
    private void rejectDuplicate(UserId owner, ComicDetails details, ComicId id) {
        comics.findByOwnerAndFingerprint(owner, details.fingerprint())
                .filter(other -> !other.id().equals(id))
                .ifPresent(other -> {
                    throw new ConflictException(
                            "library.duplicate_file", "Ese archivo ya esta en tu biblioteca como '"
                                    + other.details().title() + "'");
                });
    }

    private static NotFoundException notFound() {
        return new NotFoundException("library.comic_not_found", "No existe ese comic en tu biblioteca");
    }
}
