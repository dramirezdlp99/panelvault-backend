package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.AnalysisJob;
import com.panelvault.backend.analysis.domain.AnalysisJobRepository;
import com.panelvault.backend.analysis.domain.AnalysisPreset;
import com.panelvault.backend.analysis.domain.AnalysisResult;
import com.panelvault.backend.analysis.domain.AnalysisResultRepository;
import com.panelvault.backend.analysis.domain.JobStatus;
import com.panelvault.backend.analysis.domain.PageHash;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de la cola y de la cache de resultados. */
@Service
public class AnalysisQueryService {

    private final AnalysisJobRepository jobs;
    private final AnalysisResultRepository results;

    public AnalysisQueryService(AnalysisJobRepository jobs, AnalysisResultRepository results) {
        this.jobs = jobs;
        this.results = results;
    }

    /**
     * Estado de un trabajo. Solo lo ve quien lo pidio: a cualquier otro se le responde 404, como si
     * no existiera, para no revelar que paginas suben los demas usuarios.
     */
    @Transactional(readOnly = true)
    public JobView job(UUID jobId, UserId requester) {
        AnalysisJob job = jobs.findById(jobId)
                .filter(j -> j.requestedBy().equals(requester))
                .orElseThrow(AnalysisQueryService::jobNotFound);
        Optional<AnalysisResult> result = job.status() == JobStatus.SUCCEEDED
                ? results.find(job.pageHash(), job.preset())
                : Optional.empty();
        return new JobView(job, result);
    }

    /** Mapa de vinetas de una pagina ya analizada, buscado por su huella. */
    @Transactional(readOnly = true)
    public AnalysisResult result(String pageHash, String preset) {
        return results.find(new PageHash(pageHash), AnalysisPreset.fromValue(preset))
                .orElseThrow(() -> new NotFoundException(
                        "analysis.result_not_found", "Esa pagina aun no tiene un mapa de vinetas"));
    }

    private static NotFoundException jobNotFound() {
        return new NotFoundException("analysis.job_not_found", "No existe ese trabajo de analisis");
    }
}
