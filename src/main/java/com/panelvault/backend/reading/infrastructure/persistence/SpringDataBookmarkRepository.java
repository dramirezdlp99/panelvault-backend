package com.panelvault.backend.reading.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de Spring Data para {@link BookmarkJpaEntity}. */
public interface SpringDataBookmarkRepository extends JpaRepository<BookmarkJpaEntity, UUID> {

    List<BookmarkJpaEntity> findByUserIdAndComicIdOrderByPageAscCreatedAtAsc(UUID userId, UUID comicId);

    long countByUserIdAndComicId(UUID userId, UUID comicId);
}
