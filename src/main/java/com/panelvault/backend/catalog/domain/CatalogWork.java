package com.panelvault.backend.catalog.domain;

import com.panelvault.backend.identity.domain.UserId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Obra del catalogo publico de comics libres (Entidad y raiz de agregado).
 *
 * <p>Nace como borrador: un CURADOR la revisa (licencia, fuente, datos) y solo entonces la publica.
 * Solo las obras publicadas aparecen en las paginas publicas.
 */
public final class CatalogWork {

    private final UUID id;
    private final WorkSlug slug;
    private WorkDetails details;
    private boolean published;
    private Instant publishedAt;
    private final UserId createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    private CatalogWork(
            UUID id,
            WorkSlug slug,
            WorkDetails details,
            boolean published,
            Instant publishedAt,
            UserId createdBy,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.slug = Objects.requireNonNull(slug);
        this.details = Objects.requireNonNull(details);
        this.published = published;
        this.publishedAt = publishedAt == null ? null : truncate(publishedAt);
        this.createdBy = createdBy;
        this.createdAt = truncate(Objects.requireNonNull(createdAt));
        this.updatedAt = truncate(Objects.requireNonNull(updatedAt));
    }

    public static CatalogWork draft(WorkSlug slug, WorkDetails details, UserId curator, Instant now) {
        return new CatalogWork(UUID.randomUUID(), slug, details, false, null, curator, now, now);
    }

    public static CatalogWork rehydrate(
            UUID id,
            WorkSlug slug,
            WorkDetails details,
            boolean published,
            Instant publishedAt,
            UserId createdBy,
            Instant createdAt,
            Instant updatedAt) {
        return new CatalogWork(id, slug, details, published, publishedAt, createdBy, createdAt, updatedAt);
    }

    public void update(WorkDetails newDetails, Instant now) {
        details = Objects.requireNonNull(newDetails);
        updatedAt = truncate(now);
    }

    public void publish(Instant now) {
        if (!published) {
            published = true;
            publishedAt = truncate(now);
            updatedAt = truncate(now);
        }
    }

    public void unpublish(Instant now) {
        if (published) {
            published = false;
            updatedAt = truncate(now);
        }
    }

    private static Instant truncate(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    public UUID id() {
        return id;
    }

    public WorkSlug slug() {
        return slug;
    }

    public WorkDetails details() {
        return details;
    }

    public boolean published() {
        return published;
    }

    public Instant publishedAt() {
        return publishedAt;
    }

    /** Puede ser nulo en las obras sembradas por la migracion inicial. */
    public UserId createdBy() {
        return createdBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
