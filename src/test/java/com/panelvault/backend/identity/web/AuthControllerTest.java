package com.panelvault.backend.identity.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.panelvault.backend.identity.application.AuthFixture;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Prueba de los endpoints de autenticacion sin base de datos ni Spring Security: controlador real,
 * casos de uso reales y manejador de errores real, todo armado en memoria por {@link AuthFixture}.
 * La seguridad HTTP completa se prueba en {@link SecurityIntegrationTest}.
 */
class AuthControllerTest {

    private AuthFixture f;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        f = new AuthFixture();
        AuthController controller = new AuthController(f.register, f.login, f.twoFactorLogin, f.refresh);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ResultActions enviar(String ruta, String json) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String registro(String email, String displayName, String password) {
        return """
                {"email": "%s", "displayName": "%s", "password": "%s"}
                """.formatted(email, displayName, password);
    }

    private static String credenciales(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    private static String refresh(String token) {
        return """
                {"refreshToken": "%s"}
                """.formatted(token);
    }

    private static String verificacion(String challengeToken, String code) {
        return """
                {"challengeToken": "%s", "code": "%s"}
                """.formatted(challengeToken, code);
    }

    private String loginYObtenerRefresh() throws Exception {
        enviar("/api/v1/auth/register", registro("mj@watson.com", "Mary Jane", AuthFixture.PASSWORD));
        String body = enviar("/api/v1/auth/login", credenciales("mj@watson.com", AuthFixture.PASSWORD))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.refreshToken");
    }

    // ------------------------------------------------------------------
    // Registro
    // ------------------------------------------------------------------
    @Test
    void registraYDevuelve201SinElHash() throws Exception {
        enviar("/api/v1/auth/register", registro("Peter@DailyBugle.com", "Peter Parker", AuthFixture.PASSWORD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("peter@dailybugle.com"))
                .andExpect(jsonPath("$.displayName").value("Peter Parker"))
                .andExpect(jsonPath("$.role").value("LECTOR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString(AuthFixture.PASSWORD))));
    }

    @Test
    void unCorreoRepetidoDevuelve409() throws Exception {
        String body = registro("mj@watson.com", "Mary Jane", AuthFixture.PASSWORD);
        enviar("/api/v1/auth/register", body).andExpect(status().isCreated());

        enviar("/api/v1/auth/register", body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("user.email_taken"));
    }

    @Test
    void unaContrasenaDebilDevuelve400ConCodigoPropio() throws Exception {
        enviar("/api/v1/auth/register", registro("gwen@stacy.com", "Gwen", "corta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("user.weak_password"));
    }

    @Test
    void camposVaciosDevuelven400ConLaListaDeCampos() throws Exception {
        enviar("/api/v1/auth/register", registro("", "", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
    }

    // ------------------------------------------------------------------
    // Login, refresh y logout
    // ------------------------------------------------------------------
    @Test
    void elLoginSinDosPasosDevuelveElParDeTokens() throws Exception {
        enviar("/api/v1/auth/register", registro("mj@watson.com", "Mary Jane", AuthFixture.PASSWORD));

        enviar("/api/v1/auth/login", credenciales("mj@watson.com", AuthFixture.PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.challengeToken").doesNotExist());
    }

    @Test
    void unLoginFallidoDevuelve401Generico() throws Exception {
        enviar("/api/v1/auth/register", registro("mj@watson.com", "Mary Jane", AuthFixture.PASSWORD));

        enviar("/api/v1/auth/login", credenciales("mj@watson.com", "ClaveEquivocada1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.invalid_credentials"));
        enviar("/api/v1/auth/login", credenciales("nadie@watson.com", AuthFixture.PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.invalid_credentials"));
    }

    @Test
    void demasiadosFallosDevuelven429ConRetryAfter() throws Exception {
        enviar("/api/v1/auth/register", registro("mj@watson.com", "Mary Jane", AuthFixture.PASSWORD));
        for (int i = 0; i < 5; i++) {
            enviar("/api/v1/auth/login", credenciales("mj@watson.com", "ClaveEquivocada1"));
        }

        enviar("/api/v1/auth/login", credenciales("mj@watson.com", AuthFixture.PASSWORD))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "900"))
                .andExpect(jsonPath("$.code").value("auth.too_many_attempts"));
    }

    @Test
    void refrescarDevuelveUnRefreshTokenNuevo() throws Exception {
        String primero = loginYObtenerRefresh();

        enviar("/api/v1/auth/refresh", refresh(primero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(not(primero)));
    }

    @Test
    void cerrarSesionDevuelve204YElTokenDejaDeServir() throws Exception {
        String token = loginYObtenerRefresh();

        enviar("/api/v1/auth/logout", refresh(token)).andExpect(status().isNoContent());
        enviar("/api/v1/auth/refresh", refresh(token)).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Segundo paso (2FA)
    // ------------------------------------------------------------------
    @Test
    void conDosPasosElLoginPideElCodigoYLuegoEntregaLosTokens() throws Exception {
        User peter = f.registrar("peter@dailybugle.com");
        AuthFixture.Activation activation = f.activarDosPasos(peter);

        String body = enviar("/api/v1/auth/login", credenciales("peter@dailybugle.com", AuthFixture.PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TWO_FACTOR_REQUIRED"))
                .andExpect(jsonPath("$.challengeToken").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String challenge = JsonPath.read(body, "$.challengeToken");

        enviar("/api/v1/auth/2fa/verify", verificacion(challenge, f.codigoSiguiente(activation.secret())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void unCodigoIncorrectoEnElSegundoPasoDevuelve401() throws Exception {
        User peter = f.registrar("peter@dailybugle.com");
        f.activarDosPasos(peter);
        String body = enviar("/api/v1/auth/login", credenciales("peter@dailybugle.com", AuthFixture.PASSWORD))
                .andReturn().getResponse().getContentAsString();
        String challenge = JsonPath.read(body, "$.challengeToken");

        enviar("/api/v1/auth/2fa/verify", verificacion(challenge, "000000"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.invalid_2fa_code"));
    }
}