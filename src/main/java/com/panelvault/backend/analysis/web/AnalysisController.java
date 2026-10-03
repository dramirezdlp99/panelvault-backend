package com.panelvault.backend.analysis.web;

import com.panelvault.backend.analysis.application.AnalysisQueryService;
import com.panelvault.backend.analysis.application.AnalysisSettings;
import com.panelvault.backend.analysis.application.SubmissionResult;
import com.panelvault.backend.analysis.application.SubmitPageService;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.InvalidInputException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de analisis de vinetas ({@code /api/v1/analysis}). Requiere sesion iniciada.
 *
 * <p>Flujo del lector:
 * <ol>
 *   <li>Calcula el SHA-256 de la pagina en el navegador y pregunta
 *       {@code GET /results/{sha256}}. Si responde 200, ya tiene el mapa sin subir nada.</li>
 *   <li>Si responde 404, sube la imagen con {@code POST /pages}: responde 200 si justo estaba en
 *       cache, o 202 con el trabajo en cola y su URL en la cabecera {@code Location}.</li>
 *   <li>Consulta {@code GET /jobs/{id}} cada pocos segundos hasta que termine.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final SubmitPageService submit;
    private final AnalysisQueryService queries;
    private final AnalysisSettings settings;

    public AnalysisController(SubmitPageService submit, AnalysisQueryService queries, AnalysisSettings settings) {
        this.submit = submit;
        this.queries = queries;
        this.settings = settings;
    }

    /**
     * Recibe los bytes de la imagen en el cuerpo (JPEG, PNG o WebP). Se leen con limite: nunca se
     * cargan en memoria mas bytes que el maximo permitido, aunque el cliente envie un archivo enorme.
     */
    @PostMapping("/pages")
    public ResponseEntity<Object> submitPage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "preset", required = false) String preset,
            HttpServletRequest request)
            throws IOException {
        byte[] image = readLimited(request);
        SubmissionResult result = submit.submit(userOf(jwt), image, preset);
        return switch (result) {
            case SubmissionResult.Ready ready -> ResponseEntity.ok(AnalysisResultResponse.from(ready.result()));
            case SubmissionResult.Queued queued -> ResponseEntity
                    .accepted()
                    .location(URI.create("/api/v1/analysis/jobs/" + queued.job().id()))
                    .body(AnalysisJobResponse.from(queued.job()));
        };
    }

    @GetMapping("/jobs/{jobId}")
    public AnalysisJobResponse job(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID jobId) {
        return AnalysisJobResponse.from(queries.job(jobId, userOf(jwt)));
    }

    @GetMapping("/results/{pageSha256}")
    public AnalysisResultResponse result(
            @PathVariable String pageSha256, @RequestParam(name = "preset", required = false) String preset) {
        return AnalysisResultResponse.from(queries.result(pageSha256, preset));
    }

    private byte[] readLimited(HttpServletRequest request) throws IOException {
        long max = settings.maxImageBytes();
        if (request.getContentLengthLong() > max) {
            throw tooLarge(max);
        }
        try (InputStream in = request.getInputStream()) {
            byte[] data = in.readNBytes((int) Math.min(max + 1, Integer.MAX_VALUE));
            if (data.length > max) {
                throw tooLarge(max);
            }
            return data;
        }
    }

    private static InvalidInputException tooLarge(long max) {
        return new InvalidInputException(
                "analysis.image_too_large", "La imagen supera el maximo de " + (max / (1024 * 1024)) + " MB");
    }

    private static UserId userOf(Jwt jwt) {
        return new UserId(UUID.fromString(jwt.getSubject()));
    }
}
