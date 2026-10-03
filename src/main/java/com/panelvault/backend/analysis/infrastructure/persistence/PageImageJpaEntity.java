package com.panelvault.backend.analysis.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Fila de {@code analysis_job_images}: la imagen de una pagina mientras espera analisis.
 *
 * <p>Va en una tabla aparte de la cola para que consultar o bloquear trabajos nunca cargue los
 * bytes de las imagenes, que pesan mucho mas que el resto de la fila.
 */
@Entity
@Table(name = "analysis_job_images")
public class PageImageJpaEntity {

    @Id
    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "content", nullable = false, updatable = false)
    private byte[] content;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private int sizeBytes;

    /** Requerido por JPA. */
    protected PageImageJpaEntity() {}

    public PageImageJpaEntity(UUID jobId, byte[] content) {
        this.jobId = jobId;
        this.content = content;
        this.sizeBytes = content.length;
    }

    public UUID getJobId() {
        return jobId;
    }

    public byte[] getContent() {
        return content;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }
}
