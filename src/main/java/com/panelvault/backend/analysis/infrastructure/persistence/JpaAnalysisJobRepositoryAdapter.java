package com.panelvault.backend.analysis.infrastructure.persistence;

import com.panelvault.backend.analysis.domain.AnalysisJob;
import com.panelvault.backend.analysis.domain.AnalysisJobRepository;
import com.panelvault.backend.analysis.domain.AnalysisPreset;
import com.panelvault.backend.analysis.domain.JobStatus;
import com.panelvault.backend.analysis.domain.PageHash;
import com.panelvault.backend.identity.domain.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptador que implementa la cola de trabajos con JPA y PostgreSQL. */
@Repository
public class JpaAnalysisJobRepositoryAdapter implements AnalysisJobRepository {

    private static final List<String> ACTIVE = List.of(JobStatus.PENDING.name(), JobStatus.RUNNING.name());

    private final SpringDataAnalysisJobRepository jpa;

    public JpaAnalysisJobRepositoryAdapter(SpringDataAnalysisJobRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AnalysisJob save(AnalysisJob job) {
        return toDomain(jpa.saveAndFlush(toEntity(job)));
    }

    @Override
    public Optional<AnalysisJob> findById(UUID id) {
        return jpa.findById(id).map(JpaAnalysisJobRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<AnalysisJob> findByIdForUpdate(UUID id) {
        return jpa.findByIdForUpdate(id).map(JpaAnalysisJobRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<AnalysisJob> findActive(PageHash pageHash, AnalysisPreset preset) {
        return jpa.findFirstByPageHashAndPresetAndStatusInOrderByCreatedAtAsc(pageHash.value(), preset.value(), ACTIVE)
                .map(JpaAnalysisJobRepositoryAdapter::toDomain);
    }

    @Override
    public long countActiveByUser(UserId userId) {
        return jpa.countByRequestedByAndStatusIn(userId.value(), ACTIVE);
    }

    @Override
    public List<AnalysisJob> lockClaimable(int limit, Instant now) {
        return jpa.lockClaimable(limit, now).stream().map(JpaAnalysisJobRepositoryAdapter::toDomain).toList();
    }

    private static AnalysisJobJpaEntity toEntity(AnalysisJob job) {
        return new AnalysisJobJpaEntity(
                job.id(),
                job.pageHash().value(),
                job.preset().value(),
                job.requestedBy().value(),
                job.status().name(),
                job.attempts(),
                job.availableAt(),
                job.leaseUntil(),
                job.lastError(),
                job.createdAt(),
                job.updatedAt());
    }

    private static AnalysisJob toDomain(AnalysisJobJpaEntity row) {
        return AnalysisJob.rehydrate(
                row.getId(),
                new PageHash(row.getPageHash()),
                AnalysisPreset.fromValue(row.getPreset()),
                new UserId(row.getRequestedBy()),
                JobStatus.valueOf(row.getStatus()),
                row.getAttempts(),
                row.getAvailableAt(),
                row.getLeaseUntil(),
                row.getLastError(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}
