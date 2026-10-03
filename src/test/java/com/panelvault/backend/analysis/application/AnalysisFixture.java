package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.RetryPolicy;
import com.panelvault.backend.identity.application.MutableClock;
import java.time.Duration;
import java.time.Instant;

/**
 * Arma el modulo de analisis en memoria (Test Fixture): cola, imagenes, cache y motor falsos, con
 * los casos de uso reales. Reintentos sin azar (espera = mitad fija) para que sean predecibles.
 */
public class AnalysisFixture {

    public static final Instant START = Instant.parse("2026-10-03T15:00:00Z");
    public static final Duration LEASE = Duration.ofMinutes(3);

    public final MutableClock clock = new MutableClock(START);
    public final InMemoryAnalysisJobRepository jobs = new InMemoryAnalysisJobRepository();
    public final InMemoryPageImageStore images = new InMemoryPageImageStore();
    public final InMemoryAnalysisResultRepository results = new InMemoryAnalysisResultRepository();
    public final FakePanelAnalyzer analyzer = new FakePanelAnalyzer();
    public final AnalysisSettings settings = new AnalysisSettings(1024 * 1024, 3, 5, LEASE);
    public final RetryPolicy retryPolicy =
            new RetryPolicy(3, Duration.ofSeconds(10), Duration.ofMinutes(5), () -> 0.0);

    public final SubmitPageService submit = new SubmitPageService(jobs, results, images, settings, clock);
    public final AnalysisQueryService queries = new AnalysisQueryService(jobs, results);
    public final AnalysisJobLifecycle lifecycle =
            new AnalysisJobLifecycle(jobs, results, images, retryPolicy, settings, clock);
    public final AnalysisProcessor processor = new AnalysisProcessor(lifecycle, images, analyzer);
}
