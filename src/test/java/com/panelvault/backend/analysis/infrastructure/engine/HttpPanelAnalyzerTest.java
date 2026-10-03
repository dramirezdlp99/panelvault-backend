package com.panelvault.backend.analysis.infrastructure.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.analysis.application.PanelAnalysisException;
import com.panelvault.backend.analysis.domain.AnalysisPreset;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prueba el cliente contra un servidor HTTP real (el que trae Java), que hace de motor de IA:
 * verifica la firma igual que el motor y responde lo que cada prueba necesita.
 */
class HttpPanelAnalyzerTest {

    private static final String SECRETO = "panelvault-test-only-engine-secret-0123456789";
    private static final byte[] IMAGEN = "bytes-de-una-pagina".getBytes(StandardCharsets.UTF_8);

    private HttpServer server;
    private final AtomicReference<Integer> estado = new AtomicReference<>(200);
    private final AtomicReference<String> cuerpo = new AtomicReference<>("{\"panelMap\":{\"panels\":[]}}");
    private final AtomicReference<String> consultaRecibida = new AtomicReference<>();
    private final AtomicReference<Boolean> firmaValida = new AtomicReference<>();

    @BeforeEach
    void arrancarMotorFalso() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> responder(exchange, estado.get(), "{\"status\":\"ok\"}"));
        server.createContext("/v1/analyze", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            String timestamp = exchange.getRequestHeaders().getFirst(HmacRequestSigner.TIMESTAMP_HEADER);
            String firma = exchange.getRequestHeaders().getFirst(HmacRequestSigner.SIGNATURE_HEADER);
            String esperada = new HmacRequestSigner(SECRETO)
                    .sign(Long.parseLong(timestamp), exchange.getRequestMethod(), exchange.getRequestURI().getPath(), body);
            firmaValida.set(esperada.equals(firma));
            consultaRecibida.set(exchange.getRequestURI().getQuery());
            responder(exchange, estado.get(), cuerpo.get());
        });
        server.start();
    }

    @AfterEach
    void detenerMotorFalso() {
        server.stop(0);
    }

    private static void responder(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private HttpPanelAnalyzer cliente(String baseUrl) {
        return new HttpPanelAnalyzer(baseUrl, new HmacRequestSigner(SECRETO), Duration.ofSeconds(2),
                Duration.ofSeconds(5), Duration.ofSeconds(5), Clock.systemUTC());
    }

    private HttpPanelAnalyzer cliente() {
        return cliente("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    private static boolean reintentable(Runnable accion) {
        try {
            accion.run();
            throw new AssertionError("Se esperaba un fallo");
        } catch (PanelAnalysisException e) {
            return e.isRetryable();
        }
    }

    @Test
    void enviaLaImagenFirmadaConElModoYDevuelveLaRespuesta() {
        String respuesta = cliente().analyze(IMAGEN, AnalysisPreset.MANGA);

        assertThat(respuesta).contains("\"panelMap\"");
        assertThat(firmaValida.get()).isTrue();
        assertThat(consultaRecibida.get()).isEqualTo("preset=manga");
    }

    @Test
    void comprobarElMotorLlamaASuRutaDeSalud() {
        cliente().ensureAvailable();
    }

    @Test
    void unaImagenRechazadaEsUnFalloDefinitivo() {
        estado.set(422);
        cuerpo.set("{\"detail\":\"El cuerpo no es una imagen valida\"}");

        assertThat(reintentable(() -> cliente().analyze(IMAGEN, AnalysisPreset.WESTERN))).isFalse();
    }

    @Test
    void unErrorDelServidorEsUnFalloTemporal() {
        estado.set(503);
        assertThat(reintentable(() -> cliente().analyze(IMAGEN, AnalysisPreset.WESTERN))).isTrue();
        assertThat(reintentable(() -> cliente().ensureAvailable())).isTrue();
    }

    @Test
    void unaRespuestaSinMapaDeVinetasSeConsideraTemporal() {
        cuerpo.set("{\"algo\":\"distinto\"}");
        assertThat(reintentable(() -> cliente().analyze(IMAGEN, AnalysisPreset.WESTERN))).isTrue();
    }

    @Test
    void sinMotorEscuchandoEsUnFalloTemporal() {
        int puerto = server.getAddress().getPort();
        server.stop(0);

        assertThatThrownBy(() -> cliente("http://127.0.0.1:" + puerto).analyze(IMAGEN, AnalysisPreset.WESTERN))
                .isInstanceOf(PanelAnalysisException.class)
                .hasMessageContaining("No se pudo conectar");
        assertThat(reintentable(() -> cliente("http://127.0.0.1:" + puerto).ensureAvailable())).isTrue();
    }
}
