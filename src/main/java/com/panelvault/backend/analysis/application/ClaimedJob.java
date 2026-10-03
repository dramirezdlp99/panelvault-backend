package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.AnalysisPreset;
import java.util.UUID;

/**
 * Trabajo que un worker acaba de tomar. {@code attempt} es el token de cerca: al terminar se
 * compara con el del trabajo para saber si este worker sigue siendo su dueno.
 */
public record ClaimedJob(UUID jobId, AnalysisPreset preset, int attempt) {}
