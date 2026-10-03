package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.AnalysisJob;
import com.panelvault.backend.analysis.domain.AnalysisResult;
import java.util.Optional;

/** Un trabajo junto con su resultado, si ya termino con exito. */
public record JobView(AnalysisJob job, Optional<AnalysisResult> result) {}
