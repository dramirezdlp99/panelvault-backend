package com.panelvault.backend.catalog.infrastructure.persistence;

import com.panelvault.backend.catalog.domain.CatalogRepository;
import com.panelvault.backend.catalog.domain.CatalogWork;
import com.panelvault.backend.catalog.domain.License;
import com.panelvault.backend.catalog.domain.WorkDetails;
import com.panelvault.backend.catalog.domain.WorkSlug;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import com.panelvault.backend.shared.persistence.LikePattern;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/** Adaptador del catalogo con JPA y PostgreSQL. */
@Repository
public class JpaCatalogRepositoryAdapter implements CatalogRepository {

    private static final String TAG_SEPARATOR = ",";
    private static final Sort BY_TITLE = Sort.by(Sort.Order.asc("title"), Sort.Order.asc("slug"));

    private final SpringDataCatalogRepository jpa;

    public JpaCatalogRepositoryAdapter(SpringDataCatalogRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<CatalogWork> findById(UUID id) {
        return jpa.findById(id).map(JpaCatalogRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<CatalogWork> findBySlug(WorkSlug slug) {
        return jpa.findBySlug(slug.value()).map(JpaCatalogRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsBySlug(WorkSlug slug) {
        return jpa.existsBySlug(slug.value());
    }

    @Override
    public CatalogWork save(CatalogWork work) {
        return toDomain(jpa.saveAndFlush(toEntity(work)));
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
        jpa.flush();
    }

    @Override
    public PageResult<CatalogWork> findPublished(String search, PageQuery page) {
        PageRequest request = PageRequest.of(page.page(), page.size(), BY_TITLE);
        Page<CatalogWorkJpaEntity> rows = isBlank(search)
                ? jpa.findByPublishedTrue(request)
                : jpa.searchPublished(LikePattern.contains(search), request);
        return toPage(rows, page);
    }

    @Override
    public PageResult<CatalogWork> findAll(String search, PageQuery page) {
        PageRequest request = PageRequest.of(page.page(), page.size(), BY_TITLE);
        Page<CatalogWorkJpaEntity> rows = isBlank(search)
                ? jpa.findAll(request)
                : jpa.searchAll(LikePattern.contains(search), request);
        return toPage(rows, page);
    }

    @Override
    public List<String> publishedSlugs() {
        return jpa.findPublishedSlugs();
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private static PageResult<CatalogWork> toPage(Page<CatalogWorkJpaEntity> rows, PageQuery page) {
        return PageResult.of(rows.getContent().stream().map(JpaCatalogRepositoryAdapter::toDomain).toList(), page,
                rows.getTotalElements());
    }

    private static CatalogWorkJpaEntity toEntity(CatalogWork work) {
        WorkDetails d = work.details();
        return new CatalogWorkJpaEntity(
                work.id(),
                work.slug().value(),
                d.title(),
                d.author(),
                d.year(),
                d.publisher(),
                d.description(),
                d.sourceUrl(),
                d.coverUrl(),
                d.license().name(),
                d.pageCount(),
                String.join(TAG_SEPARATOR, d.tags()),
                work.published(),
                work.publishedAt(),
                work.createdBy() == null ? null : work.createdBy().value(),
                work.createdAt(),
                work.updatedAt());
    }

    private static CatalogWork toDomain(CatalogWorkJpaEntity row) {
        Set<String> tags = row.getTags().isEmpty()
                ? Set.of()
                : new LinkedHashSet<>(Arrays.asList(row.getTags().split(TAG_SEPARATOR)));
        WorkDetails details = new WorkDetails(
                row.getTitle(),
                row.getAuthor(),
                row.getPublicationYear(),
                row.getPublisher(),
                row.getDescription(),
                row.getSourceUrl(),
                row.getCoverUrl(),
                License.valueOf(row.getLicense()),
                row.getPageCount(),
                tags);
        return CatalogWork.rehydrate(
                row.getId(),
                new WorkSlug(row.getSlug()),
                details,
                row.isPublished(),
                row.getPublishedAt(),
                row.getCreatedBy() == null ? null : new UserId(row.getCreatedBy()),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}
