package com.panelvault.backend.reading.application;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.reading.domain.ReadingProgress;
import com.panelvault.backend.reading.domain.ReadingProgressRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Progreso de lectura en memoria para pruebas (Fake). */
public class InMemoryReadingProgressRepository implements ReadingProgressRepository {

    private final Map<String, ReadingProgress> rows = new LinkedHashMap<>();

    @Override
    public Optional<ReadingProgress> find(UserId user, ComicId comic) {
        return Optional.ofNullable(rows.get(key(user, comic))).map(InMemoryReadingProgressRepository::copy);
    }

    @Override
    public Optional<ReadingProgress> findForUpdate(UserId user, ComicId comic) {
        return find(user, comic);
    }

    @Override
    public ReadingProgress save(ReadingProgress progress) {
        rows.put(key(progress.user(), progress.comic()), copy(progress));
        return progress;
    }

    @Override
    public List<ReadingProgress> findRecent(UserId user, int limit) {
        return rows.values().stream()
                .filter(p -> p.user().equals(user))
                .sorted(Comparator.comparing(ReadingProgress::serverUpdatedAt).reversed())
                .limit(limit)
                .map(InMemoryReadingProgressRepository::copy)
                .toList();
    }

    @Override
    public long countStarted(UserId user) {
        return rows.values().stream().filter(p -> p.user().equals(user)).count();
    }

    @Override
    public long countFinished(UserId user) {
        return rows.values().stream().filter(p -> p.user().equals(user) && p.isFinished()).count();
    }

    private static String key(UserId user, ComicId comic) {
        return user + "/" + comic;
    }

    private static ReadingProgress copy(ReadingProgress p) {
        return ReadingProgress.rehydrate(p.user(), p.comic(), p.currentPage(), p.totalPages(), p.currentPanel(),
                p.guidedMode(), p.clientUpdatedAt(), p.deviceId(), p.serverUpdatedAt());
    }
}
