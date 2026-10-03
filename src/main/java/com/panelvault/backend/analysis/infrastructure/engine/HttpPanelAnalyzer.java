package com.panelvault.backend.analysis.infrastructure.engine;

import com.panelvault.backend.analysis.application.PanelAnalysisException;
import com.panelvault.backend.analysis.application.PanelAnalyzer;
import com.panelvault.backend.analysis.domain.AnalysisPreset;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cliente HTTP del motor de IA (adaptador del puerto {@link PanelAnalyzer}).
 *
 * <p>Usa el {@link HttpClient} de Java, sin librerias extra. Clasifica cada fallo:
 * <ul>
 *   <li><b>Temporal</b> (se reintenta): no hay conexion, se agoto el tiempo, 408, 429 o 5xx. Pasa
 *       cuando el motor esta dormido en Render, arrancando o sobrecargado.</li>
 *   <li><b>Definitivo</b> (no se reintenta): 4xx, por ejemplo 422 cuando la imagen esta corrupta.
 *       Reintentar daria el mismo error.</li>
 * </ul>
 */
public class HttpPanelAnalyzer implements PanelAnalyzer {

    static final String ANALYZE_PATH = "/v1/analyze";
    static final String HEALTH_PATH = "/health";

    private static final Logger log = LoggerFactory.getLogger(HttpPanelAnalyzer.class);

    private final HttpClient http;
    private final URI baseUri;
    private final HmacRequestSigner signer;
    private final Duration requestTimeout;
    private final Duration wakeUpTimeout;
    private final Clock clock;

    public HttpPanelAnalyzer(
            String baseUrl,
            HmacRequestSigner signer,
            Duration connectTimeout,
            Duration requestTimeout,
            Duration wakeUpTimeout,
            Clock clock) {
        this.baseUri = URI.create(stripTrailingSlash(Objects.requireNonNull(baseUrl)));
        this.signer = Objects.requireNonNull(signer);
        this.requestTimeout = Objects.requireNonNull(requestTimeout);
        this.wakeUpTimeout = Objects.requireNonNull(wakeUpTimeout);
        this.clock = Objects.requireNonNull(clock);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Objects.requireNonNull(connectTimeout))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public void ensureAvailable() {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(HEALTH_PATH))
                .timeout(wakeUpTimeout)
                .GET()
                .build();
        HttpResponse<String> response = send(request, "comprobar el motor de IA");
        if (response.statusCode() != 200) {
            throw new PanelAnalysisException(
                    "El motor de IA respondio " + response.statusCode() + " al comprobar su estado", true);
        }
    }

    @Override
    public String analyze(byte[] image, AnalysisPreset preset) {
        long timestamp = clock.instant().getEpochSecond();
        String signature = signer.sign(timestamp, "POST", ANALYZE_PATH, image);
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(ANALYZE_PATH + "?preset=" + preset.value()))
                .timeout(requestTimeout)
                .header("Content-Type", "application/octet-stream")
                .header(HmacRequestSigner.TIMESTAMP_HEADER, Long.toString(timestamp))
                .header(HmacRequestSigner.SIGNATURE_HEADER, signature)
                .POST(HttpRequest.BodyPublishers.ofByteArray(image))
                .build();

        HttpResponse<String> response = send(request, "analizar la pagina");
        int status = response.statusCode();
        if (status == 200) {
            String body = response.body();
            if (body == null || !body.contains("\"panelMap\"")) {
                throw new PanelAnalysisException("El motor de IA respondio sin mapa de vinetas", true);
            }
            return body;
        }
        if (status == 401) {
            log.error("El motor de IA rechazo la firma: revisa que PANELVAULT_ENGINE_SECRET sea igual en ambos servicios");
        }
        boolean retryable = status == 408 || status == 429 || status >= 500;
        throw new PanelAnalysisException("El motor de IA respondio " + status + ": " + shorten(response.body()), retryable);
    }

    private HttpResponse<String> send(HttpRequest request, String action) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new PanelAnalysisException("El motor de IA tardo demasiado al " + action, true, e);
        } catch (IOException e) {
            throw new PanelAnalysisException("No se pudo conectar con el motor de IA al " + action, true, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PanelAnalysisException("Se interrumpio la llamada al motor de IA", true, e);
        }
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String shorten(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 200 ? text : text.substring(0, 200);
    }
}
