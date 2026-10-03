package com.panelvault.backend.catalog.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de Spring Data para {@link CatalogWorkJpaEntity}. */
public interface SpringDataCatalogRepository extends JpaRepository<CatalogWorkJpaEntity, UUID> {

    Optional<CatalogWorkJpaEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<CatalogWorkJpaEntity> findByPublishedTrue(Pageable pageable);

    @Query("""
            select w from CatalogWorkJpaEntity w
            where w.published = true
              and (lower(w.title) like :pattern escape '\\' or lower(w.author) like :pattern escape '\\')
            """)
    Page<CatalogWorkJpaEntity> searchPublished(@Param("pattern") String pattern, Pageable pageable);

    @Query("""
            select w from CatalogWorkJpaEntity w
            where lower(w.title) like :pattern escape '\\' or lower(w.author) like :pattern escape '\\'
            """)
    Page<CatalogWorkJpaEntity> searchAll(@Param("pattern") String pattern, Pageable pageable);

    @Query("select w.slug from CatalogWorkJpaEntity w where w.published = true order by w.slug")
    List<String> findPublishedSlugs();
}
