package com.panelvault.backend.analysis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;

/** Fila de {@code analysis_results}: la cache de mapas de vinetas. */
@Entity
@Table(name = "analysis_results")
@IdClass(AnalysisResultId.class)
public class AnalysisResultJpaEntity {

    @Id
    @Column(name = "page_hash", nullable = false, updatable = false, length = 64)
    private String pageHash;

    @Id
    @Column(name = "preset", nullable = false, updatable = false, length = 16)
    private String preset;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Requerido por JPA. */
    protected AnalysisResultJpaEntity() {}

    public AnalysisResultJpaEntity(String pageHash, String preset, String payload, Instant createdAt) {
        this.pageHash = pageHash;
        this.preset = preset;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public String getPageHash() {
        return pageHash;
    }

    public String getPreset() {
        return preset;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
