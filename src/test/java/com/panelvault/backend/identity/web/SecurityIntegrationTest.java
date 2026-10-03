package com.panelvault.backend.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.panelvault.backend.shared.crypto.Base32;
import com.panelvault.backend.shared.crypto.Totp;
import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.Role;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
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
 * Prueba de punta a punta de la seguridad: aplicacion completa, filtros de Spring Security reales,
 * JWT firmados de verdad, cifrado AES real, TOTP real y PostgreSQL. Cada prueba se deshace al
 * terminar.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    private static final String CLAVE = "Telarana2026";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository users;

    @Autowired
    private Clock clock;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------
    private static String correoUnico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8) + "@panelvault.test";
    }

    private ResultActions postJson(String ruta, String json) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String registrar(String correo) throws Exception {
        String body = postJson("/api/v1/auth/register", """
                {"email": "%s", "displayName": "Usuario Prueba", "password": "%s"}
                """.formatted(correo, CLAVE))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String loginJson(String correo) throws Exception {
        return postJson("/api/v1/auth/login", """
                {"email": "%s", "password": "%s"}
                """.formatted(correo, CLAVE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String accessToken(String correo) throws Exception {
        return JsonPath.read(loginJson(correo), "$.accessToken");
    }

    private ResultActions refrescar(String refreshToken) throws Exception {
        return postJson("/api/v1/auth/refresh", """
                {"refreshToken": "%s"}
                """.formatted(refreshToken));
    }

    private String postConToken(String ruta, String token, String json) throws Exception {
        return mvc.perform(post(ruta)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Lo que mostraria la app autenticadora en el intervalo actual + desplazamiento. */
    private static String codigoDeLaApp(byte[] secreto, int desplazamiento) {
        return Totp.code(secreto, Totp.timeStep(Instant.now()) + desplazamiento);
    }

    private void convertirEnAdmin(String correo) {
        User user = users.findByEmail(new Email(correo)).orElseThrow();
        user.changeRole(Role.ADMIN, clock.instant());
        users.save(user);
    }

    // ------------------------------------------------------------------
    // Rutas publicas y rechazo sin credenciales
    // ------------------------------------------------------------------
    @Test
    void laSaludDelServicioEsPublica() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void sinTokenSeRespondeUn401ConElFormatoUniforme() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("auth.unauthenticated"))
                .andExpect(jsonPath("$.path").value("/api/v1/me"));
    }

    @Test
    void unTokenFalsoSeRechazaCon401() throws Exception {
        mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.sirve"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.invalid_token"));
    }

    // ------------------------------------------------------------------
    // Flujo normal
    // ------------------------------------------------------------------
    @Test
    void registroLoginYConsultaDelPerfil() throws Exception {
        String correo = correoUnico("peter");
        registrar(correo);
        String token = accessToken(correo);

        mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(correo))
                .andExpect(jsonPath("$.role").value("LECTOR"));
    }

    // ------------------------------------------------------------------
    // RBAC
    // ------------------------------------------------------------------
    @Test
    void unLectorNoPuedeAdministrarUsuarios() throws Exception {
        String correo = correoUnico("lector");
        String id = registrar(correo);
        String token = accessToken(correo);

        mvc.perform(patch("/api/v1/admin/users/" + id + "/role")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void unAdminPuedeCambiarElRolDeOtroUsuario() throws Exception {
        String correoAdmin = correoUnico("admin");
        registrar(correoAdmin);
        convertirEnAdmin(correoAdmin);
        String tokenAdmin = accessToken(correoAdmin);
        String idObjetivo = registrar(correoUnico("mj"));

        mvc.perform(patch("/api/v1/admin/users/" + idObjetivo + "/role")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\": \"CURADOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CURADOR"));
    }

    // ------------------------------------------------------------------
    // Rotacion y deteccion de reuso por HTTP
    // ------------------------------------------------------------------
    @Test
    void reusarUnRefreshTokenCierraLaSesionCompleta() throws Exception {
        String correo = correoUnico("gwen");
        registrar(correo);
        String primero = JsonPath.read(loginJson(correo), "$.refreshToken");

        String respuesta = refrescar(primero).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String segundo = JsonPath.read(respuesta, "$.refreshToken");

        refrescar(primero)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.refresh_token_reused"));
        refrescar(segundo)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.refresh_token_reused"));
    }

    // ------------------------------------------------------------------
    // Verificacion en dos pasos (2FA)
    // ------------------------------------------------------------------
    @Test
    void flujoCompletoDeDosPasosPorHttp() throws Exception {
        String correo = correoUnico("miles");
        registrar(correo);
        String token = accessToken(correo);

        // 1. Activar: el usuario "escanea el QR" (aqui, decodifica el secreto) y confirma.
        String setup = postConToken("/api/v1/me/2fa/setup", token, "{}");
        byte[] secreto = Base32.decode(JsonPath.read(setup, "$.secret"));
        String confirmacion = postConToken(
                "/api/v1/me/2fa/confirm", token, "{\"code\": \"" + codigoDeLaApp(secreto, 0) + "\"}");
        List<String> codigosDeRecuperacion = JsonPath.read(confirmacion, "$.recoveryCodes");
        assertThat(codigosDeRecuperacion).hasSize(10);

        mvc.perform(get("/api/v1/me/2fa").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.recoveryCodesRemaining").value(10));

        // 2. Login: la contrasena ya no basta.
        String login = loginJson(correo);
        assertThat(JsonPath.<String>read(login, "$.status")).isEqualTo("TWO_FACTOR_REQUIRED");
        String ticket = JsonPath.read(login, "$.challengeToken");

        // 3. Segundo paso con el codigo del intervalo siguiente (el actual ya se uso al confirmar).
        String tokens = postJson("/api/v1/auth/2fa/verify", """
                {"challengeToken": "%s", "code": "%s"}
                """.formatted(ticket, codigoDeLaApp(secreto, 1)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String nuevoAccessToken = JsonPath.read(tokens, "$.accessToken");

        mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + nuevoAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(correo));
    }

    @Test
    void elTicketDelSegundoPasoNoSirveComoAccessToken() throws Exception {
        String correo = correoUnico("ticket");
        registrar(correo);
        String token = accessToken(correo);
        byte[] secreto = Base32.decode(JsonPath.read(postConToken("/api/v1/me/2fa/setup", token, "{}"), "$.secret"));
        postConToken("/api/v1/me/2fa/confirm", token, "{\"code\": \"" + codigoDeLaApp(secreto, 0) + "\"}");
        String ticket = JsonPath.read(loginJson(correo), "$.challengeToken");

        mvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + ticket))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.invalid_token"));
    }

    @Test
    void lasRutasDeDosPasosExigenSesion() throws Exception {
        mvc.perform(get("/api/v1/me/2fa")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/me/2fa/setup")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Limite de intentos
    // ------------------------------------------------------------------
    @Test
    void demasiadosIntentosDeLoginDevuelven429() throws Exception {
        String correo = correoUnico("bruto");
        registrar(correo);
        String malo = """
                {"email": "%s", "password": "ClaveEquivocada1"}
                """.formatted(correo);
        for (int i = 0; i < 5; i++) {
            postJson("/api/v1/auth/login", malo).andExpect(status().isUnauthorized());
        }

        postJson("/api/v1/auth/login", malo)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.code").value("auth.too_many_attempts"));
    }
}