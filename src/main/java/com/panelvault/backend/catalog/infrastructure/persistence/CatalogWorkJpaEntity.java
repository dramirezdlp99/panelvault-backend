package com.panelvault.backend.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Fila de {@code catalog_works} (creada por Flyway en V7). */
@Entity
@Table(name = "catalog_works")
public class CatalogWorkJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "slug", nullable = false, updatable = false, length = 80)
    private String slug;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "author", nullable = false, length = 120)
    private String author;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "publisher", length = 120)
    private String publisher;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "license", nullable = false, length = 20)
    private String license;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "tags", nullable = false, length = 400)
    private String tags;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Requerido por JPA. */
    protected CatalogWorkJpaEntity() {}

    public CatalogWorkJpaEntity(
            UUID id,
            String slug,
            String title,
            String author,
            Integer publicationYear,
            String publisher,
            String description,
            String sourceUrl,
            String coverUrl,
            String license,
            Integer pageCount,
            String tags,
            boolean published,
            Instant publishedAt,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.author = author;
        this.publicationYear = publicationYear;
        this.publisher = publisher;
        this.description = description;
        this.sourceUrl = sourceUrl;
        this.coverUrl = coverUrl;
        this.license = license;
        this.pageCount = pageCount;
        this.tags = tags;
        this.published = published;
        this.publishedAt = publishedAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getLicense() {
        return license;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public String getTags() {
        return tags;
    }

    public boolean isPublished() {
        return published;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
