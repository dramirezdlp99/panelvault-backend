package com.panelvault.backend.library.infrastructure.persistence;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.Comic;
import com.panelvault.backend.library.domain.ComicDetails;
import com.panelvault.backend.library.domain.ComicFormat;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.library.domain.ComicRepository;
import com.panelvault.backend.library.domain.FileFingerprint;
import com.panelvault.backend.library.domain.ReadingDirection;
import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/** Adaptador de la biblioteca con JPA y PostgreSQL. */
@Repository
public class JpaComicRepositoryAdapter implements ComicRepository {

    private static final String TAG_SEPARATOR = ",";

    private final SpringDataComicRepository jpa;

    public JpaComicRepositoryAdapter(SpringDataComicRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Comic> findById(ComicId id) {
        return jpa.findById(id.value()).map(JpaComicRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Comic> findByOwnerAndFingerprint(UserId owner, FileFingerprint fingerprint) {
        return jpa.findByOwnerIdAndFileSha256(owner.value(), fingerprint.value()).map(JpaComicRepositoryAdapter::toDomain);
    }

    @Override
    public PageResult<Comic> findByOwner(UserId owner, String search, PageQuery page) {
        PageRequest request = PageRequest.of(
                page.page(), page.size(), Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id")));
        Page<ComicJpaEntity> rows = search == null || search.isBlank()
                ? jpa.findByOwnerId(owner.value(), request)
                : jpa.search(owner.value(), likePattern(search), request);
        return PageResult.of(rows.getContent().stream().map(JpaComicRepositoryAdapter::toDomain).toList(), page,
                rows.getTotalElements());
    }

    @Override
    public Comic save(Comic comic) {
        return toDomain(jpa.saveAndFlush(toEntity(comic)));
    }

    @Override
    public void delete(ComicId id) {
        jpa.deleteById(id.value());
        jpa.flush();
    }

    @Override
    public long countByOwner(UserId owner) {
        return jpa.countByOwnerId(owner.value());
    }

    @Override
    public long totalPagesByOwner(UserId owner) {
        return jpa.sumPageCountByOwnerId(owner.value());
    }

    /**
     * Convierte el texto buscado en un patron LIKE seguro. Sin escapar, alguien que busque "100%"
     * o "a_b" estaria usando los comodines de SQL ({@code %} y {@code _}) sin querer.
     */
    static String likePattern(String search) {
        String escaped = search.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static ComicJpaEntity toEntity(Comic comic) {
        ComicDetails d = comic.details();
        return new ComicJpaEntity(
                comic.id().value(),
                comic.owner().value(),
                d.title(),
                d.series(),
                d.issueNumber(),
                d.pageCount(),
                d.format().name(),
                d.fingerprint().value(),
                d.direction().name(),
                String.join(TAG_SEPARATOR, d.tags()),
                comic.createdAt(),
                comic.updatedAt(),
                comic.version());
    }

    private static Comic toDomain(ComicJpaEntity row) {
        Set<String> tags = row.getTags().isEmpty()
                ? Set.of()
                : new LinkedHashSet<>(Arrays.asList(row.getTags().split(TAG_SEPARATOR)));
        ComicDetails details = new ComicDetails(
                row.getTitle(),
                row.getSeries(),
                row.getIssueNumber(),
                row.getPageCount(),
                ComicFormat.valueOf(row.getFormat()),
                new FileFingerprint(row.getFileSha256()),
                ReadingDirection.valueOf(row.getReadingDirection()),
                tags);
        return Comic.rehydrate(
                new ComicId(row.getId()),
                new UserId(row.getOwnerId()),
                details,
                row.getCreatedAt(),
                row.getUpdatedAt(),
                row.getVersion());
    }
}
