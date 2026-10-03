package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.AnalysisJob;
import com.panelvault.backend.analysis.domain.AnalysisJobRepository;
import com.panelvault.backend.analysis.domain.AnalysisResult;
import com.panelvault.backend.analysis.domain.AnalysisResultRepository;
import com.panelvault.backend.analysis.domain.JobStatus;
import com.panelvault.backend.analysis.domain.PageImageStore;
import com.panelvault.backend.analysis.domain.RetryPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transiciones de la cola, cada una en su propia transaccion corta.
 *
 * <p>Esta separada de {@link AnalysisProcessor} a proposito: la llamada al motor de IA puede tardar
 * decenas de segundos y NO debe ocurrir dentro de una transaccion (tendria filas bloqueadas y una
 * conexion ocupada todo ese tiempo). Asi el flujo es: transaccion corta para tomar, llamada larga
 * sin transaccion, transaccion corta para registrar el resultado.
 */
@Service
public class AnalysisJobLifecycle {

    private static final Logger log = LoggerFactory.getLogger(AnalysisJobLifecycle.class);

    private final AnalysisJobRepository jobs;
    private final AnalysisResultRepository results;
    private final PageImageStore images;
    private final RetryPolicy retryPolicy;
    private final AnalysisSettings settings;
    private final Clock clock;

    public AnalysisJobLifecycle(
            AnalysisJobRepository jobs,
            AnalysisResultRepository results,
            PageImageStore images,
            RetryPolicy retryPolicy,
            AnalysisSettings settings,
            Clock clock) {
        this.jobs = jobs;
        this.results = results;
        this.images = images;
        this.retryPolicy = retryPolicy;
        this.settings = settings;
        this.clock = clock;
    }

    /** Toma un lote de trabajos disponibles y los marca en curso. */
    @Transactional
    public List<ClaimedJob> claimBatch() {
        Instant now = clock.instant();
        return jobs.lockClaimable(settings.batchSize(), now).stream()
                .map(job -> {
                    job.claim(now, settings.lease());
                    jobs.save(job);
                    return new ClaimedJob(job.id(), job.preset(), job.attempts());
                })
                .toList();
    }

    @Transactional
    public void recordSuccess(ClaimedJob claimed, String payloadJson) {
        Instant now = clock.instant();
        Optional<AnalysisJob> owned = ownedJob(claimed);
        if (owned.isEmpty()) {
            return;
        }
        AnalysisJob job = owned.get();
        results.save(new AnalysisResult(job.pageHash(), job.preset(), payloadJson, now));
        job.succeed(now);
        jobs.save(job);
        images.delete(job.id());
    }

    @Transactional
    public void recordFailure(ClaimedJob claimed, String error, boolean retryable) {
        Instant now = clock.instant();
        Optional<AnalysisJob> owned = ownedJob(claimed);
        if (owned.isEmpty()) {
            return;
        }
        AnalysisJob job = owned.get();
        Optional<Duration> retryIn = retryable ? retryPolicy.nextDelay(job.attempts()) : Optional.empty();
        job.fail(error, retryIn, now);
        jobs.save(job);
        if (job.status() == JobStatus.FAILED) {
            images.delete(job.id());
            log.warn("Analisis fallido definitivamente: {} ({})", job, error);
        }
    }

    /** El trabajo, solo si este worker sigue siendo su dueno (token de cerca). */
    private Optional<AnalysisJob> ownedJob(ClaimedJob claimed) {
        Optional<AnalysisJob> job = jobs.findByIdForUpdate(claimed.jobId())
                .filter(j -> j.holdsLease(claimed.attempt()));
        if (job.isEmpty()) {
            log.info("Se descarta el resultado del trabajo {}: su arrendamiento ya no es de este worker",
                    claimed.jobId());
        }
        return job;
    }
}
