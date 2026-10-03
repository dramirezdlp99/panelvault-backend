package com.panelvault.backend.reading.infrastructure.persistence;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.reading.domain.Bookmark;
import com.panelvault.backend.reading.domain.BookmarkRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptador de los marcadores con JPA y PostgreSQL. */
@Repository
public class JpaBookmarkRepositoryAdapter implements BookmarkRepository {

    private final SpringDataBookmarkRepository jpa;

    public JpaBookmarkRepositoryAdapter(SpringDataBookmarkRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Bookmark> findById(UUID id) {
        return jpa.findById(id).map(JpaBookmarkRepositoryAdapter::toDomain);
    }

    @Override
    public List<Bookmark> findByComic(UserId user, ComicId comic) {
        return jpa.findByUserIdAndComicIdOrderByPageAscCreatedAtAsc(user.value(), comic.value()).stream()
                .map(JpaBookmarkRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public long countByComic(UserId user, ComicId comic) {
        return jpa.countByUserIdAndComicId(user.value(), comic.value());
    }

    @Override
    public Bookmark save(Bookmark bookmark) {
        return toDomain(jpa.saveAndFlush(new BookmarkJpaEntity(
                bookmark.id(),
                bookmark.user().value(),
                bookmark.comic().value(),
                bookmark.page(),
                bookmark.note(),
                bookmark.createdAt())));
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
        jpa.flush();
    }

    private static Bookmark toDomain(BookmarkJpaEntity row) {
        return Bookmark.rehydrate(
                row.getId(),
                new UserId(row.getUserId()),
                new ComicId(row.getComicId()),
                row.getPage(),
                row.getNote(),
                row.getCreatedAt());
    }
}
