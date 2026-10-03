package com.panelvault.backend.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de Spring Data para {@link AnalysisResultJpaEntity}. */
public interface SpringDataAnalysisResultRepository extends JpaRepository<AnalysisResultJpaEntity, AnalysisResultId> {}
