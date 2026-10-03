package com.panelvault.backend.analysis.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.panelvault.backend.analysis.application.AnalysisJobLifecycle;
import com.panelvault.backend.analysis.application.ClaimedJob;
import com.panelvault.backend.analysis.domain.PageHash;
import com.panelvault.backend.analysis.domain.TestImages;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Prueba de punta a punta de la API de analisis: seguridad real, PostgreSQL y cola real. El worker
 * esta apagado en las pruebas; el procesamiento se simula registrando el resultado a mano, como lo
 * haria el worker tras recibir la respuesta del motor.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalysisApiIntegrationTest {

    private static final String RESPUESTA_MOTOR =
            "{\"engineVersion\":\"test\",\"panelMap\":{\"schemaVersion\":1,\"panels\":[]}}";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AnalysisJobLifecycle lifecycle;

    private MockMvc mvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String correo = "lector-" + UUID.randomUUID().toString().substring(0, 8) + "@panelvault.test";
        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"displayName\":\"Lector\",\"password\":\"Telarana2026\"}".formatted(correo)));
        String login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Telarana2026\"}".formatted(correo)))
                .andReturn().getResponse().getContentAsString();
        token = JsonPath.read(login, "$.accessToken");
    }

    private ResultActions subir(byte[] imagen, String preset) throws Exception {
        return mvc.perform(post("/api/v1/analysis/pages")
                .param("preset", preset)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.IMAGE_PNG)
                .content(imagen));
    }

    /** Hace lo que haria el worker con el trabajo indicado: tomarlo y registrar la respuesta del motor. */
    private void procesar(String jobId) {
        ClaimedJob claimed = lifecycle.claimBatch().stream()
                .filter(c -> c.jobId().toString().equals(jobId))
                .findFirst()
                .orElseThrow();
        lifecycle.recordSuccess(claimed, RESPUESTA_MOTOR);
    }

    @Test
    void sinSesionNoSePuedeUsarLaApi() throws Exception {
        mvc.perform(post("/api/v1/analysis/pages").contentType(MediaType.IMAGE_PNG).content(TestImages.PNG))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void flujoCompletoSubirConsultarYObtenerElMapa() throws Exception {
        byte[] pagina = TestImages.pngVariant(1);
        String hash = PageHash.of(pagina).value();

        // 1. Antes de subirla no hay resultado.
        mvc.perform(get("/api/v1/analysis/results/" + hash).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("analysis.result_not_found"));

        // 2. Subirla la deja en cola: 202 con la URL del trabajo.
        String cuerpo = subir(pagina, "western")
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/v1/analysis/jobs/")))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.pageSha256").value(hash))
                .andReturn().getResponse().getContentAsString();
        String jobId = JsonPath.read(cuerpo, "$.jobId");

        // 3. Subirla de nuevo mientras espera devuelve el mismo trabajo.
        String repetida = subir(pagina, "western").andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(repetida, "$.jobId")).isEqualTo(jobId);

        // 4. El worker la procesa.
        procesar(jobId);

        // 5. El trabajo termino y trae el mapa como JSON anidado (no como texto).
        mvc.perform(get("/api/v1/analysis/jobs/" + jobId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.result.panelMap.schemaVersion").value(1));

        // 6. Ahora la cache responde directo, por huella y al volver a subirla.
        mvc.perform(get("/api/v1/analysis/results/" + hash).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.engineVersion").value("test"));
        subir(pagina, "western")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSha256").value(hash));
    }

    @Test
    void rechazaContenidoQueNoEsImagen() throws Exception {
        subir("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8), "western")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("analysis.unsupported_image"));
    }

    @Test
    void rechazaUnModoDeLecturaDesconocido() throws Exception {
        subir(TestImages.PNG, "vertical")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("analysis.unknown_preset"));
    }

    @Test
    void unTrabajoAjenoNoSeVe() throws Exception {
        mvc.perform(get("/api/v1/analysis/jobs/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("analysis.job_not_found"));
    }
}
