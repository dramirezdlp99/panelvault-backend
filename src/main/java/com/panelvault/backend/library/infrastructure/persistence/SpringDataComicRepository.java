package com.panelvault.backend.library.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de Spring Data para {@link ComicJpaEntity}. */
public interface SpringDataComicRepository extends JpaRepository<ComicJpaEntity, UUID> {

    Optional<ComicJpaEntity> findByOwnerIdAndFileSha256(UUID ownerId, String fileSha256);

    Page<ComicJpaEntity> findByOwnerId(UUID ownerId, Pageable pageable);

    /**
     * Busqueda por titulo o serie sin distinguir mayusculas. El patron llega con los comodines de
     * LIKE ya escapados (ver {@link JpaComicRepositoryAdapter#likePattern}).
     */
    @Query("""
            select c from ComicJpaEntity c
            where c.ownerId = :ownerId
              and (lower(c.title) like :pattern escape '\\'
                   or lower(coalesce(c.series, '')) like :pattern escape '\\')
            """)
    Page<ComicJpaEntity> search(@Param("ownerId") UUID ownerId, @Param("pattern") String pattern, Pageable pageable);

    long countByOwnerId(UUID ownerId);

    @Query("select coalesce(sum(c.pageCount), 0) from ComicJpaEntity c where c.ownerId = :ownerId")
    long sumPageCountByOwnerId(@Param("ownerId") UUID ownerId);
}
