package com.panelvault.backend.analysis.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de Spring Data para {@link AnalysisJobJpaEntity}. */
public interface SpringDataAnalysisJobRepository extends JpaRepository<AnalysisJobJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from AnalysisJobJpaEntity j where j.id = :id")
    Optional<AnalysisJobJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<AnalysisJobJpaEntity> findFirstByPageHashAndPresetAndStatusInOrderByCreatedAtAsc(
            String pageHash, String preset, Collection<String> statuses);

    long countByRequestedByAndStatusIn(UUID requestedBy, Collection<String> statuses);

    /**
     * Toma trabajos disponibles: pendientes cuya hora ya llego, o en curso con el lease vencido.
     *
     * <p>{@code FOR UPDATE SKIP LOCKED} bloquea las filas elegidas y salta las que otro worker ya
     * tiene bloqueadas. Es SQL nativo porque JPQL no tiene SKIP LOCKED.
     */
    @Query(
            nativeQuery = true,
            value = """
                    SELECT * FROM analysis_jobs
                    WHERE (status = 'PENDING' AND available_at <= :now)
                       OR (status = 'RUNNING' AND lease_until < :now)
                    ORDER BY available_at
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """)
    List<AnalysisJobJpaEntity> lockClaimable(@Param("limit") int limit, @Param("now") Instant now);
}
