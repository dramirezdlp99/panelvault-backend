package com.panelvault.backend.analysis.infrastructure.engine;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades {@code panelvault.analysis.*} de application.yaml.
 *
 * @param engine conexion con el motor de IA
 * @param worker procesamiento de la cola
 * @param retry  reintentos ante fallos temporales
 * @param limits limites de las paginas subidas
 */
@ConfigurationProperties(prefix = "panelvault.analysis")
public record AnalysisProperties(Engine engine, Worker worker, Retry retry, Limits limits) {

    /**
     * @param baseUrl        URL del motor (en local, http://localhost:8001)
     * @param secret         secreto HMAC compartido con el motor
     * @param connectTimeout tiempo maximo para conectar
     * @param requestTimeout tiempo maximo para analizar una pagina
     * @param wakeUpTimeout  tiempo maximo para que el motor despierte (Render Free tarda en arrancar)
     */
    public record Engine(
            String baseUrl, String secret, Duration connectTimeout, Duration requestTimeout, Duration wakeUpTimeout) {

        @Override
        public String toString() {
            return "Engine[baseUrl=" + baseUrl + ", secret=***]";
        }
    }

    /**
     * @param enabled      si el worker programado corre (en las pruebas se apaga)
     * @param pollInterval espera entre vueltas del worker
     * @param batchSize    trabajos por vuelta
     * @param lease        duracion del arrendamiento de un trabajo en curso
     */
    public record Worker(boolean enabled, Duration pollInterval, int batchSize, Duration lease) {}

    public record Retry(int maxAttempts, Duration baseDelay, Duration maxDelay) {}

    public record Limits(long maxImageBytes, int maxActiveJobsPerUser) {}
}
