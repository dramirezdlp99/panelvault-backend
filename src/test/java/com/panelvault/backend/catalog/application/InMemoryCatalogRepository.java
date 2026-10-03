package com.panelvault.backend.catalog.application;

import com.panelvault.backend.catalog.domain.CatalogRepository;
import com.panelvault.backend.catalog.domain.CatalogWork;
import com.panelvault.backend.catalog.domain.WorkSlug;
import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/** Catalogo en memoria para pruebas (Fake). */
public class InMemoryCatalogRepository implements CatalogRepository {

    private final Map<UUID, CatalogWork> works = new LinkedHashMap<>();

    @Override
    public Optional<CatalogWork> findById(UUID id) {
        return Optional.ofNullable(works.get(id)).map(InMemoryCatalogRepository::copy);
    }

    @Override
    public Optional<CatalogWork> findBySlug(WorkSlug slug) {
        return works.values().stream().filter(w -> w.slug().equals(slug)).findFirst().map(InMemoryCatalogRepository::copy);
    }

    @Override
    public boolean existsBySlug(WorkSlug slug) {
        return works.values().stream().anyMatch(w -> w.slug().equals(slug));
    }

    @Override
    public CatalogWork save(CatalogWork work) {
        works.put(work.id(), copy(work));
        return work;
    }

    @Override
    public void delete(UUID id) {
        works.remove(id);
    }

    @Override
    public PageResult<CatalogWork> findPublished(String search, PageQuery page) {
        return page(w -> w.published() && matches(w, search), page);
    }

    @Override
    public PageResult<CatalogWork> findAll(String search, PageQuery page) {
        return page(w -> matches(w, search), page);
    }

    @Override
    public List<String> publishedSlugs() {
        return works.values().stream().filter(CatalogWork::published).map(w -> w.slug().value()).sorted().toList();
    }

    private PageResult<CatalogWork> page(Predicate<CatalogWork> filter, PageQuery page) {
        List<CatalogWork> all = works.values().stream()
                .filter(filter)
                .sorted(Comparator.comparing(w -> w.details().title()))
                .toList();
        return PageResult.of(all.stream().skip(page.offset()).limit(page.size()).map(InMemoryCatalogRepository::copy).toList(),
                page, all.size());
    }

    private static boolean matches(CatalogWork w, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = search.strip().toLowerCase(Locale.ROOT);
        return w.details().title().toLowerCase(Locale.ROOT).contains(needle)
                || w.details().author().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static CatalogWork copy(CatalogWork w) {
        return CatalogWork.rehydrate(w.id(), w.slug(), w.details(), w.published(), w.publishedAt(), w.createdBy(),
                w.createdAt(), w.updatedAt());
    }
}
