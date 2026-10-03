package com.panelvault.backend.analysis.infrastructure.engine;

import com.panelvault.backend.analysis.application.AnalysisSettings;
import com.panelvault.backend.analysis.application.PanelAnalyzer;
import com.panelvault.backend.analysis.domain.RetryPolicy;
import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Arma el modulo de analisis a partir de las propiedades: cliente del motor, reintentos y limites. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AnalysisProperties.class)
public class AnalysisConfiguration {

    @Bean
    public PanelAnalyzer panelAnalyzer(AnalysisProperties properties, Clock clock) {
        AnalysisProperties.Engine engine = properties.engine();
        return new HttpPanelAnalyzer(
                engine.baseUrl(),
                new HmacRequestSigner(engine.secret()),
                engine.connectTimeout(),
                engine.requestTimeout(),
                engine.wakeUpTimeout(),
                clock);
    }

    @Bean
    public RetryPolicy analysisRetryPolicy(AnalysisProperties properties) {
        AnalysisProperties.Retry retry = properties.retry();
        return new RetryPolicy(
                retry.maxAttempts(),
                retry.baseDelay(),
                retry.maxDelay(),
                () -> ThreadLocalRandom.current().nextDouble());
    }

    @Bean
    public AnalysisSettings analysisSettings(AnalysisProperties properties) {
        return new AnalysisSettings(
                properties.limits().maxImageBytes(),
                properties.limits().maxActiveJobsPerUser(),
                properties.worker().batchSize(),
                properties.worker().lease());
    }
}
