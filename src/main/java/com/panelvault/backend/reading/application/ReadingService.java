package com.panelvault.backend.reading.application;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.Comic;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.library.domain.ComicRepository;
import com.panelvault.backend.reading.domain.ProgressUpdate;
import com.panelvault.backend.reading.domain.ReadingProgress;
import com.panelvault.backend.reading.domain.ReadingProgressRepository;
import com.panelvault.backend.shared.error.InvalidInputException;
import com.panelvault.backend.shared.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso del progreso de lectura entre dispositivos.
 *
 * <p>Las pestanas de un mismo navegador se sincronizan entre si en el frontend (BroadcastChannel),
 * sin pasar por aqui; este servicio sincroniza entre dispositivos distintos (celular, portatil).
 */
@Service
public class ReadingService {

    public static final int MAX_RECENT = 20;

    private final ReadingProgressRepository progress;
    private final ComicRepository comics;
    private final Clock clock;

    public ReadingService(ReadingProgressRepository progress, ComicRepository comics, Clock clock) {
        this.progress = progress;
        this.comics = comics;
        this.clock = clock;
    }

    @Transactional
    public SyncResult sync(UserId user, ComicId comicId, ProgressUpdate rawUpdate) {
        Comic comic = ownedComic(user, comicId);
        int pages = comic.details().pageCount();
        Instant now = clock.instant();
        ProgressUpdate update = rawUpdate.clampedTo(now);
        if (update.currentPage() > pages) {
            throw new InvalidInputException("reading.invalid_page", "La pagina debe estar entre 1 y " + pages);
        }

        Optional<ReadingProgress> stored = progress.findForUpdate(user, comicId);
        if (stored.isEmpty()) {
            return new SyncResult(progress.save(ReadingProgress.start(user, comicId, pages, update, now)), true);
        }
        ReadingProgress current = stored.get();
        boolean applied = current.merge(update, pages, now);
        if (applied) {
            progress.save(current);
        }
        return new SyncResult(current, applied);
    }

    @Transactional(readOnly = true)
    public ReadingProgress get(UserId user, ComicId comicId) {
        ownedComic(user, comicId);
        return progress.find(user, comicId)
                .orElseThrow(() -> new NotFoundException("reading.no_progress", "Aun no has empezado este comic"));
    }

    @Transactional(readOnly = true)
    public List<RecentReading> recent(UserId user, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_RECENT));
        return progress.findRecent(user, safeLimit).stream()
                .flatMap(p -> comics.findById(p.comic()).map(c -> new RecentReading(c, p)).stream())
                .toList();
    }

    private Comic ownedComic(UserId user, ComicId comicId) {
        return comics.findById(comicId)
                .filter(c -> c.isOwnedBy(user))
                .orElseThrow(() -> new NotFoundException("library.comic_not_found", "No existe ese comic en tu biblioteca"));
    }
}
