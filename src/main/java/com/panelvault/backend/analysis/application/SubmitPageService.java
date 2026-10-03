package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.AnalysisJob;
import com.panelvault.backend.analysis.domain.AnalysisJobRepository;
import com.panelvault.backend.analysis.domain.AnalysisPreset;
import com.panelvault.backend.analysis.domain.AnalysisResultRepository;
import com.panelvault.backend.analysis.domain.ImageFormat;
import com.panelvault.backend.analysis.domain.PageHash;
import com.panelvault.backend.analysis.domain.PageImageStore;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.InvalidInputException;
import com.panelvault.backend.shared.error.TooManyRequestsException;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: un usuario sube una pagina para que el motor de IA detecte sus vinetas.
 *
 * <p>Orden de decisiones (de lo mas barato a lo mas caro):
 * <ol>
 *   <li>Validar: no vacia, no demasiado grande, y que de verdad sea JPEG, PNG o WebP.</li>
 *   <li>Calcular la huella SHA-256 (la calcula el servidor; no se confia en la del cliente).</li>
 *   <li>Si ya hay resultado en la cache, devolverlo de inmediato: no se gasta motor.</li>
 *   <li>Si la misma pagina ya esta en la cola, devolver ese trabajo: no se duplica trabajo.</li>
 *   <li>Si no, encolarla, salvo que el usuario ya tenga demasiados trabajos pendientes.</li>
 * </ol>
 */
@Service
public class SubmitPageService {

    private final AnalysisJobRepository jobs;
    private final AnalysisResultRepository results;
    private final PageImageStore images;
    private final AnalysisSettings settings;
    private final Clock clock;

    public SubmitPageService(
            AnalysisJobRepository jobs,
            AnalysisResultRepository results,
            PageImageStore images,
            AnalysisSettings settings,
            Clock clock) {
        this.jobs = jobs;
        this.results = results;
        this.images = images;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional
    public SubmissionResult submit(UserId requester, byte[] image, String rawPreset) {
        AnalysisPreset preset = AnalysisPreset.fromValue(rawPreset);
        validate(image);
        PageHash hash = PageHash.of(image);

        var cached = results.find(hash, preset);
        if (cached.isPresent()) {
            return new SubmissionResult.Ready(cached.get());
        }
        Optional<AnalysisJob> active = jobs.findActive(hash, preset);
        if (active.isPresent()) {
            return new SubmissionResult.Queued(active.get());
        }
        if (jobs.countActiveByUser(requester) >= settings.maxActiveJobsPerUser()) {
            throw new TooManyRequestsException(
                    "analysis.too_many_pending",
                    "Tienes demasiadas paginas en espera de analisis. Intenta de nuevo en un minuto",
                    60);
        }
        AnalysisJob job = jobs.save(AnalysisJob.enqueue(hash, preset, requester, clock.instant()));
        images.save(job.id(), image);
        return new SubmissionResult.Queued(job);
    }

    private void validate(byte[] image) {
        if (image == null || image.length == 0) {
            throw new InvalidInputException("analysis.empty_image", "No se recibio ninguna imagen");
        }
        if (image.length > settings.maxImageBytes()) {
            throw new InvalidInputException(
                    "analysis.image_too_large",
                    "La imagen supera el maximo de " + (settings.maxImageBytes() / (1024 * 1024)) + " MB");
        }
        if (ImageFormat.detect(image).isEmpty()) {
            throw new InvalidInputException(
                    "analysis.unsupported_image", "Solo se aceptan imagenes JPEG, PNG o WebP");
        }
    }
}
