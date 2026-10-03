package com.panelvault.backend.catalog.domain;

import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida del catalogo. */
public interface CatalogRepository {

    Optional<CatalogWork> findById(UUID id);

    Optional<CatalogWork> findBySlug(WorkSlug slug);

    boolean existsBySlug(WorkSlug slug);

    CatalogWork save(CatalogWork work);

    void delete(UUID id);

    /** Obras visibles al publico, ordenadas por titulo. {@code search} filtra por titulo o autor. */
    PageResult<CatalogWork> findPublished(String search, PageQuery page);

    /** Todas las obras, incluidos los borradores, para los curadores. */
    PageResult<CatalogWork> findAll(String search, PageQuery page);

    /** Slugs publicados: el frontend los usa para pregenerar las paginas del catalogo. */
    List<String> publishedSlugs();
}
