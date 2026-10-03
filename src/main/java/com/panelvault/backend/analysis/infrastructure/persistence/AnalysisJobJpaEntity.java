package com.panelvault.backend.analysis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Fila de la tabla {@code analysis_jobs} (la cola de trabajos, creada por Flyway en V4). */
@Entity
@Table(name = "analysis_jobs")
public class AnalysisJobJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "page_hash", nullable = false, updatable = false, length = 64)
    private String pageHash;

    @Column(name = "preset", nullable = false, updatable = false, length = 16)
    private String preset;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedBy;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Requerido por JPA. */
    protected AnalysisJobJpaEntity() {}

    public AnalysisJobJpaEntity(
            UUID id,
            String pageHash,
            String preset,
            UUID requestedBy,
            String status,
            int attempts,
            Instant availableAt,
            Instant leaseUntil,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.pageHash = pageHash;
        this.preset = preset;
        this.requestedBy = requestedBy;
        this.status = status;
        this.attempts = attempts;
        this.availableAt = availableAt;
        this.leaseUntil = leaseUntil;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getPageHash() {
        return pageHash;
    }

    public String getPreset() {
        return preset;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public String getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public Instant getLeaseUntil() {
        return leaseUntil;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
