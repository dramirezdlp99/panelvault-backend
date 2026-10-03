package com.panelvault.backend.analysis.infrastructure.worker;

import com.panelvault.backend.analysis.application.AnalysisProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Worker programado: cada {@code poll-interval} procesa un lote de la cola.
 *
 * <p>Si una vuelta proceso un lote completo, es probable que haya mas trabajo esperando, asi que
 * sigue sin esperar; cuando la cola se vacia, descansa hasta la siguiente vuelta.
 */
public class AnalysisWorker {

    private static final Logger log = LoggerFactory.getLogger(AnalysisWorker.class);
    /** Limite de lotes seguidos en una vuelta, para no acaparar el hilo del planificador. */
    static final int MAX_BATCHES_PER_TICK = 20;

    private final AnalysisProcessor processor;
    private final int batchSize;

    public AnalysisWorker(AnalysisProcessor processor, int batchSize) {
        this.processor = processor;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${panelvault.analysis.worker.poll-interval}")
    public void tick() {
        try {
            for (int i = 0; i < MAX_BATCHES_PER_TICK; i++) {
                if (processor.runOnce() < batchSize) {
                    return;
                }
            }
        } catch (RuntimeException e) {
            // Un error aqui (por ejemplo, la base de datos no responde) no debe matar el planificador.
            log.error("Fallo una vuelta del worker de analisis", e);
        }
    }
}
