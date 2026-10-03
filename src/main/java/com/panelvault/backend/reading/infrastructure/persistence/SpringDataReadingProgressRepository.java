package com.panelvault.backend.reading.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de Spring Data para {@link ReadingProgressJpaEntity}. */
public interface SpringDataReadingProgressRepository
        extends JpaRepository<ReadingProgressJpaEntity, ReadingProgressId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ReadingProgressJpaEntity p where p.userId = :userId and p.comicId = :comicId")
    Optional<ReadingProgressJpaEntity> findForUpdate(@Param("userId") UUID userId, @Param("comicId") UUID comicId);

    List<ReadingProgressJpaEntity> findByUserIdOrderByServerUpdatedAtDesc(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    long countByUserIdAndFinishedTrue(UUID userId);
}
