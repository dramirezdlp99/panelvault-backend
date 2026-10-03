package com.panelvault.backend.reading.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Fila de {@code reading_progress} (creada por Flyway en V6). */
@Entity
@Table(name = "reading_progress")
@IdClass(ReadingProgressId.class)
public class ReadingProgressJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Id
    @Column(name = "comic_id", nullable = false, updatable = false)
    private UUID comicId;

    @Column(name = "current_page", nullable = false)
    private int currentPage;

    @Column(name = "total_pages", nullable = false)
    private int totalPages;

    @Column(name = "current_panel", nullable = false)
    private int currentPanel;

    @Column(name = "guided_mode", nullable = false)
    private boolean guidedMode;

    @Column(name = "finished", nullable = false)
    private boolean finished;

    @Column(name = "client_updated_at", nullable = false)
    private Instant clientUpdatedAt;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "server_updated_at", nullable = false)
    private Instant serverUpdatedAt;

    /** Requerido por JPA. */
    protected ReadingProgressJpaEntity() {}

    public ReadingProgressJpaEntity(
            UUID userId,
            UUID comicId,
            int currentPage,
            int totalPages,
            int currentPanel,
            boolean guidedMode,
            boolean finished,
            Instant clientUpdatedAt,
            String deviceId,
            Instant serverUpdatedAt) {
        this.userId = userId;
        this.comicId = comicId;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.currentPanel = currentPanel;
        this.guidedMode = guidedMode;
        this.finished = finished;
        this.clientUpdatedAt = clientUpdatedAt;
        this.deviceId = deviceId;
        this.serverUpdatedAt = serverUpdatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getComicId() {
        return comicId;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getCurrentPanel() {
        return currentPanel;
    }

    public boolean isGuidedMode() {
        return guidedMode;
    }

    public boolean isFinished() {
        return finished;
    }

    public Instant getClientUpdatedAt() {
        return clientUpdatedAt;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Instant getServerUpdatedAt() {
        return serverUpdatedAt;
    }
}
