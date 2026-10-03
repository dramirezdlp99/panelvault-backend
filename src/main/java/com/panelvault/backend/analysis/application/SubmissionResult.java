package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.AnalysisJob;
import com.panelvault.backend.analysis.domain.AnalysisResult;

/** Resultado de subir una pagina: ya estaba analizada, o quedo (o ya estaba) en la cola. */
public sealed interface SubmissionResult {

    /** La pagina ya estaba en la cache: el mapa de vinetas esta listo. */
    record Ready(AnalysisResult result) implements SubmissionResult {}

    /** La pagina esta en la cola; hay que consultar el trabajo hasta que termine. */
    record Queued(AnalysisJob job) implements SubmissionResult {}
}
