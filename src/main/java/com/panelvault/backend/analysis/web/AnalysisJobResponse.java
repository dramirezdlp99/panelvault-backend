package com.panelvault.backend.analysis.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.panelvault.backend.analysis.application.JobView;
import com.panelvault.backend.analysis.domain.AnalysisJob;
import java.time.Instant;
import java.util.UUID;

/**
 * Estado de un trabajo de analisis. El cliente lo consulta hasta que {@code status} sea
 * {@code SUCCEEDED} (trae {@code result}) o {@code FAILED} (trae {@code lastError}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalysisJobResponse(
        UUID jobId,
        String pageSha256,
        String preset,
        String status,
        int attempts,
        String lastError,
        Instant createdAt,
        @JsonRawValue String result) {

    public static AnalysisJobResponse from(AnalysisJob job) {
        return new AnalysisJobResponse(
                job.id(),
                job.pageHash().value(),
                job.preset().value(),
                job.status().name(),
                job.attempts(),
                job.lastError(),
                job.createdAt(),
                null);
    }

    public static AnalysisJobResponse from(JobView view) {
        AnalysisJob job = view.job();
        return new AnalysisJobResponse(
                job.id(),
                job.pageHash().value(),
                job.preset().value(),
                job.status().name(),
                job.attempts(),
                job.lastError(),
                job.createdAt(),
                view.result().map(r -> r.payloadJson()).orElse(null));
    }
}
