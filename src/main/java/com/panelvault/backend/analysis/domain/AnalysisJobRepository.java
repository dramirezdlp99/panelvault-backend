package com.panelvault.backend.analysis.domain;

import com.panelvault.backend.identity.domain.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida de la cola de trabajos. */
public interface AnalysisJobRepository {

    AnalysisJob save(AnalysisJob job);

    Optional<AnalysisJob> findById(UUID id);

    /** Igual que {@link #findById} pero bloquea la fila hasta el fin de la transaccion. */
    Optional<AnalysisJob> findByIdForUpdate(UUID id);

    /** Trabajo aun activo (pendiente o en curso) para la misma pagina y modo, si existe. */
    Optional<AnalysisJob> findActive(PageHash pageHash, AnalysisPreset preset);

    /** Cuantos trabajos activos tiene un usuario (para limitar abusos). */
    long countActiveByUser(UserId userId);

    /**
     * Toma hasta {@code limit} trabajos disponibles y los bloquea para esta transaccion.
     *
     * <p>En PostgreSQL usa {@code SELECT ... FOR UPDATE SKIP LOCKED}: si otro worker ya bloqueo una
     * fila, esta consulta la salta en vez de esperarla. Asi varios workers pueden trabajar en
     * paralelo sobre la misma tabla sin tomar nunca el mismo trabajo.
     */
    List<AnalysisJob> lockClaimable(int limit, Instant now);
}
