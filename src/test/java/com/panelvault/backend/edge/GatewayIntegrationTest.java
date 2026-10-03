package com.panelvault.backend.edge;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Con el gateway activo, como en produccion: solo pasan las peticiones firmadas por el servidor de
 * Next.js. Usa su propia configuracion (el resto de las pruebas corre con el gateway apagado).
 */
@SpringBootTest(properties = {
    "panelvault.edge.gateway.enabled=true",
    "panelvault.edge.gateway.secret=" + GatewayIntegrationTest.SECRETO
})
@ActiveProfiles("test")
@Transactional
class GatewayIntegrationTest {

    static final String SECRETO = "panelvault-test-only-gateway-secret-0123456789";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;
    private final RequestSignatureVerifier signer = new RequestSignatureVerifier(SECRETO, Duration.ofMinutes(5));

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** Firma como lo hara el servidor de Next.js. */
    private MockHttpServletRequestBuilder firmar(
            MockHttpServletRequestBuilder request, String method, String pathAndQuery, byte[] body, Instant when) {
        String ts = Long.toString(when.getEpochSecond());
        return request.header(RequestSignatureVerifier.TIMESTAMP_HEADER, ts)
                .header(RequestSignatureVerifier.SIGNATURE_HEADER, signer.sign(ts, method, pathAndQuery, body));
    }

    @Test
    void laSaludDelServicioNoExigeFirma() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void sinFirmaSeRechazaAunqueLaRutaSeaPublica() throws Exception {
        mvc.perform(get("/api/v1/catalog/works"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("gateway.missing_signature"));
    }

    @Test
    void conFirmaValidaPasa() throws Exception {
        String ruta = "/api/v1/catalog/works?q=nemo";
        mvc.perform(firmar(get(ruta), "GET", ruta, new byte[0], Instant.now()))
                .andExpect(status().isOk());
    }

    @Test
    void unaFirmaFalsaOVencidaSeRechaza() throws Exception {
        String ruta = "/api/v1/catalog/works";
        mvc.perform(get(ruta)
                        .header(RequestSignatureVerifier.TIMESTAMP_HEADER, Long.toString(Instant.now().getEpochSecond()))
                        .header(RequestSignatureVerifier.SIGNATURE_HEADER, "0".repeat(64)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("gateway.invalid_signature"));

        mvc.perform(firmar(get(ruta), "GET", ruta, new byte[0], Instant.now().minus(Duration.ofMinutes(10))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("gateway.expired_signature"));
    }

    @Test
    void elCuerpoFirmadoLlegaIntactoAlControladorYNoSePuedeAlterar() throws Exception {
        String correo = "gateway-" + UUID.randomUUID().toString().substring(0, 8) + "@panelvault.test";
        byte[] cuerpo = "{\"email\":\"%s\",\"displayName\":\"Gateway\",\"password\":\"Telarana2026\"}"
                .formatted(correo).getBytes(StandardCharsets.UTF_8);
        String ruta = "/api/v1/auth/register";

        mvc.perform(firmar(post(ruta), "POST", ruta, cuerpo, Instant.now())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(correo));

        byte[] alterado = new String(cuerpo, StandardCharsets.UTF_8).replace("Gateway", "Atacante")
                .getBytes(StandardCharsets.UTF_8);
        mvc.perform(firmar(post(ruta), "POST", ruta, cuerpo, Instant.now())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("gateway.invalid_signature"));
    }
}
