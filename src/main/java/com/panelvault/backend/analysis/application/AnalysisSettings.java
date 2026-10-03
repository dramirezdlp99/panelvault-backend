package com.panelvault.backend.analysis.application;

import java.time.Duration;

/**
 * Limites y tiempos de la cola de analisis.
 *
 * @param maxImageBytes        tamano maximo de una pagina subida
 * @param maxActiveJobsPerUser trabajos pendientes o en curso que puede tener un usuario a la vez
 * @param batchSize            trabajos que toma el worker en cada vuelta
 * @param lease                cuanto dura el arrendamiento de un trabajo en curso
 */
public record AnalysisSettings(long maxImageBytes, int maxActiveJobsPerUser, int batchSize, Duration lease) {

    public AnalysisSettings {
        if (maxImageBytes <= 0 || maxActiveJobsPerUser <= 0 || batchSize <= 0) {
            throw new IllegalArgumentException("Los limites de analisis deben ser positivos");
        }
        if (lease == null || lease.isNegative() || lease.isZero()) {
            throw new IllegalArgumentException("El arrendamiento debe ser positivo");
        }
    }
}
