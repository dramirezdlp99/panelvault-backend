package com.panelvault.backend.reading.domain;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.ComicId;
import java.util.List;
import java.util.Optional;

/** Puerto de salida del progreso de lectura. */
public interface ReadingProgressRepository {

    Optional<ReadingProgress> find(UserId user, ComicId comic);

    /** Bloquea la fila: dos dispositivos que sincronizan a la vez no pueden pisarse. */
    Optional<ReadingProgress> findForUpdate(UserId user, ComicId comic);

    ReadingProgress save(ReadingProgress progress);

    /** Los ultimos comics leidos, del mas reciente al mas antiguo. */
    List<ReadingProgress> findRecent(UserId user, int limit);

    long countStarted(UserId user);

    long countFinished(UserId user);
}
