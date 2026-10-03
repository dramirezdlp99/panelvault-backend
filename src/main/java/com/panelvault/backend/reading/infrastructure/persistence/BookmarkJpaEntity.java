package com.panelvault.backend.reading.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Fila de {@code bookmarks} (creada por Flyway en V6). */
@Entity
@Table(name = "bookmarks")
public class BookmarkJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "comic_id", nullable = false, updatable = false)
    private UUID comicId;

    @Column(name = "page", nullable = false)
    private int page;

    @Column(name = "note", nullable = false, length = 200)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Requerido por JPA. */
    protected BookmarkJpaEntity() {}

    public BookmarkJpaEntity(UUID id, UUID userId, UUID comicId, int page, String note, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.comicId = comicId;
        this.page = page;
        this.note = note;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getComicId() {
        return comicId;
    }

    public int getPage() {
        return page;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
