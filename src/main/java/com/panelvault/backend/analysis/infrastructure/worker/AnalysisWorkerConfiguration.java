package com.panelvault.backend.analysis.infrastructure.worker;

import com.panelvault.backend.analysis.application.AnalysisProcessor;
import com.panelvault.backend.analysis.infrastructure.engine.AnalysisProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa el worker solo si {@code panelvault.analysis.worker.enabled=true}. En las pruebas se apaga
 * para que nada procese la cola por su cuenta mientras una prueba la revisa.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "panelvault.analysis.worker", name = "enabled", havingValue = "true")
public class AnalysisWorkerConfiguration {

    @Bean
    public AnalysisWorker analysisWorker(AnalysisProcessor processor, AnalysisProperties properties) {
        return new AnalysisWorker(processor, properties.worker().batchSize());
    }
}
