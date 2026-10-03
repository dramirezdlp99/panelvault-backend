package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.AnalysisPreset;

/**
 * Puerto hacia el motor de IA que detecta vinetas.
 *
 * <p>La aplicacion no sabe que el motor es un servicio Python con FastAPI al otro lado de HTTP;
 * solo sabe que entrega un JSON con el mapa de vinetas o falla con {@link PanelAnalysisException}.
 */
public interface PanelAnalyzer {

    /**
     * Despierta y comprueba el motor antes de un lote de trabajos. En Render Free el servicio se
     * duerme tras un rato sin uso y tarda en arrancar; mejor esperarlo una vez que fallar N veces.
     */
    default void ensureAvailable() {}

    /** Analiza una imagen y devuelve la respuesta del motor en JSON. */
    String analyze(byte[] image, AnalysisPreset preset);
}
