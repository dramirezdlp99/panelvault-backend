package com.panelvault.backend.analysis.infrastructure.persistence;

import com.panelvault.backend.analysis.domain.AnalysisPreset;
import com.panelvault.backend.analysis.domain.AnalysisResult;
import com.panelvault.backend.analysis.domain.AnalysisResultRepository;
import com.panelvault.backend.analysis.domain.PageHash;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador de la cache de resultados con JPA y PostgreSQL. */
@Repository
public class JpaAnalysisResultRepositoryAdapter implements AnalysisResultRepository {

    private final SpringDataAnalysisResultRepository jpa;

    public JpaAnalysisResultRepositoryAdapter(SpringDataAnalysisResultRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<AnalysisResult> find(PageHash pageHash, AnalysisPreset preset) {
        return jpa.findById(new AnalysisResultId(pageHash.value(), preset.value()))
                .map(JpaAnalysisResultRepositoryAdapter::toDomain);
    }

    @Override
    public AnalysisResult save(AnalysisResult result) {
        return toDomain(jpa.saveAndFlush(new AnalysisResultJpaEntity(
                result.pageHash().value(), result.preset().value(), result.payloadJson(), result.createdAt())));
    }

    private static AnalysisResult toDomain(AnalysisResultJpaEntity row) {
        return new AnalysisResult(
                new PageHash(row.getPageHash()),
                AnalysisPreset.fromValue(row.getPreset()),
                row.getPayload(),
                row.getCreatedAt());
    }
}
