package com.panelvault.backend.reading.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Clave compuesta de {@link ReadingProgressJpaEntity}: usuario y comic. */
public class ReadingProgressId implements Serializable {

    private UUID userId;
    private UUID comicId;

    /** Requerido por JPA. */
    protected ReadingProgressId() {}

    public ReadingProgressId(UUID userId, UUID comicId) {
        this.userId = userId;
        this.comicId = comicId;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof ReadingProgressId that
                        && Objects.equals(userId, that.userId)
                        && Objects.equals(comicId, that.comicId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, comicId);
    }
}
