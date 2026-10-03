package com.panelvault.backend.analysis.domain;

import java.util.Optional;

/** Puerto de la cache de resultados, indexada por huella de pagina y modo de lectura. */
public interface AnalysisResultRepository {

    Optional<AnalysisResult> find(PageHash pageHash, AnalysisPreset preset);

    /** Guarda o reemplaza el resultado de esa pagina y modo. */
    AnalysisResult save(AnalysisResult result);
}
