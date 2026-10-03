package com.panelvault.backend.analysis.domain;

import com.panelvault.backend.identity.domain.UserId;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Trabajo de analisis de una pagina (Entidad). Es una fila de la cola de trabajos.
 *
 * <p><b>Lease (arrendamiento):</b> cuando un worker toma el trabajo, lo "arrienda" por un tiempo.
 * Si el worker muere a mitad (por ejemplo, Render reinicia el servicio), el lease vence y otro
 * worker lo puede retomar. Nada se queda atascado para siempre en RUNNING.
 *
 * <p><b>Token de cerca (fencing token):</b> el numero de intento identifica cada arrendamiento. Si
 * un worker lento termina despues de que su lease vencio y otro worker ya retomo el trabajo, su
 * resultado se descarta: {@link #holdsLease(int)} le dice que ya no es el dueno.
 */
public final class AnalysisJob {

    /** Largo maximo del ultimo error guardado (coincide con la columna). */
    public static final int MAX_ERROR_LENGTH = 500;

    private final UUID id;
    private final PageHash pageHash;
    private final AnalysisPreset preset;
    private final UserId requestedBy;
    private JobStatus status;
    private int attempts;
    private Instant availableAt;
    private Instant leaseUntil;
    private String lastError;
    private final Instant createdAt;
    private Instant updatedAt;

    private AnalysisJob(
            UUID id,
            PageHash pageHash,
            AnalysisPreset preset,
            UserId requestedBy,
            JobStatus status,
            int attempts,
            Instant availableAt,
            Instant leaseUntil,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.pageHash = Objects.requireNonNull(pageHash);
        this.preset = Objects.requireNonNull(preset);
        this.requestedBy = Objects.requireNonNull(requestedBy);
        this.status = Objects.requireNonNull(status);
        if (attempts < 0) {
            throw new IllegalArgumentException("Los intentos no pueden ser negativos");
        }
        this.attempts = attempts;
        this.availableAt = truncate(Objects.requireNonNull(availableAt));
        this.leaseUntil = leaseUntil == null ? null : truncate(leaseUntil);
        this.lastError = lastError;
        this.createdAt = truncate(Objects.requireNonNull(createdAt));
        this.updatedAt = truncate(Objects.requireNonNull(updatedAt));
    }

    /** Encola una pagina: queda pendiente y disponible de inmediato. */
    public static AnalysisJob enqueue(PageHash pageHash, AnalysisPreset preset, UserId requestedBy, Instant now) {
        return new AnalysisJob(
                UUID.randomUUID(), pageHash, preset, requestedBy, JobStatus.PENDING, 0, now, null, null, now, now);
    }

    public static AnalysisJob rehydrate(
            UUID id,
            PageHash pageHash,
            AnalysisPreset preset,
            UserId requestedBy,
            JobStatus status,
            int attempts,
            Instant availableAt,
            Instant leaseUntil,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        return new AnalysisJob(
                id, pageHash, preset, requestedBy, status, attempts, availableAt, leaseUntil, lastError, createdAt,
                updatedAt);
    }

    /** Puede tomarse si esta pendiente y ya llego su hora, o si su lease vencio. */
    public boolean isClaimable(Instant now) {
        return (status == JobStatus.PENDING && !availableAt.isAfter(now))
                || (status == JobStatus.RUNNING && leaseUntil != null && leaseUntil.isBefore(now));
    }

    /** Un worker toma el trabajo: pasa a RUNNING, cuenta un intento y arrienda por {@code lease}. */
    public void claim(Instant now, Duration lease) {
        if (!isClaimable(now)) {
            throw new IllegalStateException("El trabajo no esta disponible para tomarse");
        }
        status = JobStatus.RUNNING;
        attempts++;
        leaseUntil = truncate(now.plus(lease));
        updatedAt = truncate(now);
    }

    /** Indica si quien tomo el trabajo en el intento {@code attempt} sigue siendo su dueno. */
    public boolean holdsLease(int attempt) {
        return status == JobStatus.RUNNING && attempts == attempt;
    }

    public void succeed(Instant now) {
        requireRunning();
        status = JobStatus.SUCCEEDED;
        leaseUntil = null;
        lastError = null;
        updatedAt = truncate(now);
    }

    /**
     * Registra un fallo. Con {@code retryIn} presente vuelve a la cola para despues; sin el, el
     * trabajo termina como FAILED.
     */
    public void fail(String error, Optional<Duration> retryIn, Instant now) {
        requireRunning();
        lastError = shorten(error);
        leaseUntil = null;
        updatedAt = truncate(now);
        if (retryIn.isPresent()) {
            status = JobStatus.PENDING;
            availableAt = truncate(now.plus(retryIn.get()));
        } else {
            status = JobStatus.FAILED;
        }
    }

    private void requireRunning() {
        if (status != JobStatus.RUNNING) {
            throw new IllegalStateException("Solo un trabajo en curso puede terminar");
        }
    }

    private static String shorten(String error) {
        String text = error == null || error.isBlank() ? "Error desconocido" : error.strip();
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH);
    }

    private static Instant truncate(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    public UUID id() {
        return id;
    }

    public PageHash pageHash() {
        return pageHash;
    }

    public AnalysisPreset preset() {
        return preset;
    }

    public UserId requestedBy() {
        return requestedBy;
    }

    public JobStatus status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }

    public Instant availableAt() {
        return availableAt;
    }

    public Instant leaseUntil() {
        return leaseUntil;
    }

    public String lastError() {
        return lastError;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof AnalysisJob that && id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "AnalysisJob[id=" + id + ", page=" + pageHash + ", preset=" + preset.value()
                + ", status=" + status + ", attempts=" + attempts + "]";
    }
}
