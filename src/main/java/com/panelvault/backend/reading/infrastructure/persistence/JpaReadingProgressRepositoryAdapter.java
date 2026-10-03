package com.panelvault.backend.reading.infrastructure.persistence;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.reading.domain.ReadingProgress;
import com.panelvault.backend.reading.domain.ReadingProgressRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/** Adaptador del progreso de lectura con JPA y PostgreSQL. */
@Repository
public class JpaReadingProgressRepositoryAdapter implements ReadingProgressRepository {

    private final SpringDataReadingProgressRepository jpa;

    public JpaReadingProgressRepositoryAdapter(SpringDataReadingProgressRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<ReadingProgress> find(UserId user, ComicId comic) {
        return jpa.findById(new ReadingProgressId(user.value(), comic.value()))
                .map(JpaReadingProgressRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<ReadingProgress> findForUpdate(UserId user, ComicId comic) {
        return jpa.findForUpdate(user.value(), comic.value()).map(JpaReadingProgressRepositoryAdapter::toDomain);
    }

    @Override
    public ReadingProgress save(ReadingProgress progress) {
        return toDomain(jpa.saveAndFlush(new ReadingProgressJpaEntity(
                progress.user().value(),
                progress.comic().value(),
                progress.currentPage(),
                progress.totalPages(),
                progress.currentPanel(),
                progress.guidedMode(),
                progress.isFinished(),
                progress.clientUpdatedAt(),
                progress.deviceId(),
                progress.serverUpdatedAt())));
    }

    @Override
    public List<ReadingProgress> findRecent(UserId user, int limit) {
        return jpa.findByUserIdOrderByServerUpdatedAtDesc(user.value(), PageRequest.of(0, limit)).stream()
                .map(JpaReadingProgressRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public long countStarted(UserId user) {
        return jpa.countByUserId(user.value());
    }

    @Override
    public long countFinished(UserId user) {
        return jpa.countByUserIdAndFinishedTrue(user.value());
    }

    private static ReadingProgress toDomain(ReadingProgressJpaEntity row) {
        return ReadingProgress.rehydrate(
                new UserId(row.getUserId()),
                new ComicId(row.getComicId()),
                row.getCurrentPage(),
                row.getTotalPages(),
                row.getCurrentPanel(),
                row.isGuidedMode(),
                row.getClientUpdatedAt(),
                row.getDeviceId(),
                row.getServerUpdatedAt());
    }
}
