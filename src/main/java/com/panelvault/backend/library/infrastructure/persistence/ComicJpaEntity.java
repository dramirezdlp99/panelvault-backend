package com.panelvault.backend.library.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Fila de la tabla {@code comics} (creada por Flyway en V5). */
@Entity
@Table(name = "comics")
public class ComicJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "series", length = 200)
    private String series;

    @Column(name = "issue_number", length = 20)
    private String issueNumber;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "format", nullable = false, length = 10)
    private String format;

    @Column(name = "file_sha256", nullable = false, length = 64)
    private String fileSha256;

    @Column(name = "reading_direction", nullable = false, length = 16)
    private String readingDirection;

    @Column(name = "tags", nullable = false, length = 400)
    private String tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "version", nullable = false)
    private long version;

    /** Requerido por JPA. */
    protected ComicJpaEntity() {}

    public ComicJpaEntity(
            UUID id,
            UUID ownerId,
            String title,
            String series,
            String issueNumber,
            int pageCount,
            String format,
            String fileSha256,
            String readingDirection,
            String tags,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.series = series;
        this.issueNumber = issueNumber;
        this.pageCount = pageCount;
        this.format = format;
        this.fileSha256 = fileSha256;
        this.readingDirection = readingDirection;
        this.tags = tags;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getTitle() {
        return title;
    }

    public String getSeries() {
        return series;
    }

    public String getIssueNumber() {
        return issueNumber;
    }

    public int getPageCount() {
        return pageCount;
    }

    public String getFormat() {
        return format;
    }

    public String getFileSha256() {
        return fileSha256;
    }

    public String getReadingDirection() {
        return readingDirection;
    }

    public String getTags() {
        return tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
