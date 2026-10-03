package com.panelvault.backend.reading.application;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.application.ReadingStatsProvider;
import com.panelvault.backend.reading.domain.ReadingProgressRepository;
import org.springframework.stereotype.Service;

/** Implementa el puerto que define la biblioteca para su resumen (inversion de dependencias). */
@Service
public class ReadingStatsService implements ReadingStatsProvider {

    private final ReadingProgressRepository progress;

    public ReadingStatsService(ReadingProgressRepository progress) {
        this.progress = progress;
    }

    @Override
    public long comicsStarted(UserId user) {
        return progress.countStarted(user);
    }

    @Override
    public long comicsFinished(UserId user) {
        return progress.countFinished(user);
    }
}
