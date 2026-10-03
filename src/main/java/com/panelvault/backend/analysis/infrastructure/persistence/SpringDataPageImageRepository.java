package com.panelvault.backend.analysis.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio de Spring Data para {@link PageImageJpaEntity}. */
public interface SpringDataPageImageRepository extends JpaRepository<PageImageJpaEntity, UUID> {}
