package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.PageImageStore;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Procesa la cola: toma un lote, llama al motor de IA por cada trabajo y registra el resultado.
 *
 * <p>Lo invoca periodicamente el worker programado. Un error en un trabajo nunca detiene el lote:
 * se registra como fallo de ese trabajo y se sigue con el siguiente.
 */
@Service
public class AnalysisProcessor {

    private static final Logger log = LoggerFactory.getLogger(AnalysisProcessor.class);

    private final AnalysisJobLifecycle lifecycle;
    private final PageImageStore images;
    private final PanelAnalyzer analyzer;

    public AnalysisProcessor(AnalysisJobLifecycle lifecycle, PageImageStore images, PanelAnalyzer analyzer) {
        this.lifecycle = lifecycle;
        this.images = images;
        this.analyzer = analyzer;
    }

    /** Una vuelta del worker. Devuelve cuantos trabajos proceso. */
    public int runOnce() {
        List<ClaimedJob> batch = lifecycle.claimBatch();
        if (batch.isEmpty()) {
            return 0;
        }
        try {
            analyzer.ensureAvailable();
        } catch (PanelAnalysisException e) {
            // El motor no responde: todo el lote vuelve a la cola con espera, sin gastar mas tiempo.
            batch.forEach(job -> lifecycle.recordFailure(job, e.getMessage(), true));
            return batch.size();
        }
        batch.forEach(this::process);
        return batch.size();
    }

    private void process(ClaimedJob job) {
        Optional<byte[]> image = images.load(job.jobId());
        if (image.isEmpty()) {
            lifecycle.recordFailure(job, "La imagen de la pagina ya no esta disponible", false);
            return;
        }
        try {
            lifecycle.recordSuccess(job, analyzer.analyze(image.get(), job.preset()));
        } catch (PanelAnalysisException e) {
            lifecycle.recordFailure(job, e.getMessage(), e.isRetryable());
        } catch (RuntimeException e) {
            log.error("Error inesperado analizando el trabajo {}", job.jobId(), e);
            lifecycle.recordFailure(job, "Error inesperado: " + e.getClass().getSimpleName(), true);
        }
    }
}
