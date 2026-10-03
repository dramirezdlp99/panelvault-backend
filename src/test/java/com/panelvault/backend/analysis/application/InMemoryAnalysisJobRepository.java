package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.AnalysisJob;
import com.panelvault.backend.analysis.domain.AnalysisJobRepository;
import com.panelvault.backend.analysis.domain.AnalysisPreset;
import com.panelvault.backend.analysis.domain.PageHash;
import com.panelvault.backend.identity.domain.UserId;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cola de trabajos en memoria para pruebas (Fake). Reproduce las mismas reglas que la consulta SQL:
 * disponibles primero por {@code availableAt}, limitado a {@code limit}. Guarda copias, como una base.
 */
public class InMemoryAnalysisJobRepository implements AnalysisJobRepository {

    private final Map<UUID, AnalysisJob> jobs = new LinkedHashMap<>();

    @Override
    public AnalysisJob save(AnalysisJob job) {
        jobs.put(job.id(), copy(job));
        return job;
    }

    @Override
    public Optional<AnalysisJob> findById(UUID id) {
        return Optional.ofNullable(jobs.get(id)).map(InMemoryAnalysisJobRepository::copy);
    }

    @Override
    public Optional<AnalysisJob> findByIdForUpdate(UUID id) {
        return findById(id);
    }

    @Override
    public Optional<AnalysisJob> findActive(PageHash pageHash, AnalysisPreset preset) {
        return jobs.values().stream()
                .filter(j -> j.pageHash().equals(pageHash) && j.preset() == preset && !j.status().isFinished())
                .min(Comparator.comparing(AnalysisJob::createdAt))
                .map(InMemoryAnalysisJobRepository::copy);
    }

    @Override
    public long countActiveByUser(UserId userId) {
        return jobs.values().stream()
                .filter(j -> j.requestedBy().equals(userId) && !j.status().isFinished())
                .count();
    }

    @Override
    public List<AnalysisJob> lockClaimable(int limit, Instant now) {
        return jobs.values().stream()
                .filter(j -> j.isClaimable(now))
                .sorted(Comparator.comparing(AnalysisJob::availableAt))
                .limit(limit)
                .map(InMemoryAnalysisJobRepository::copy)
                .toList();
    }

    public List<AnalysisJob> all() {
        return jobs.values().stream().map(InMemoryAnalysisJobRepository::copy).toList();
    }

    private static AnalysisJob copy(AnalysisJob j) {
        return AnalysisJob.rehydrate(j.id(), j.pageHash(), j.preset(), j.requestedBy(), j.status(), j.attempts(),
                j.availableAt(), j.leaseUntil(), j.lastError(), j.createdAt(), j.updatedAt());
    }
}
