package com.panelvault.backend.identity.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.panelvault.backend.identity.application.FakeAccessTokenIssuer;
import com.panelvault.backend.identity.application.FakePasswordHasher;
import com.panelvault.backend.identity.application.InMemoryRefreshTokenRepository;
import com.panelvault.backend.identity.application.InMemoryUserRepository;
import com.panelvault.backend.identity.application.LoginService;
import com.panelvault.backend.identity.application.MutableClock;
import com.panelvault.backend.identity.application.OpaqueTokenGenerator;
import com.panelvault.backend.identity.application.RegisterUserService;
import com.panelvault.backend.identity.application.SessionTokenService;
import com.panelvault.backend.identity.application.TokenRefreshService;
import com.panelvault.backend.shared.web.GlobalExceptionHandler;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Prueba de los endpoints de autenticacion sin base de datos ni Spring Security: controlador real,
 * casos de uso reales y manejador de errores real, con repositorios y emisores en memoria.
 * La seguridad HTTP completa se prueba en {@link SecurityIntegrationTest}.
 */
class AuthControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        InMemoryRefreshTokenRepository refreshTokens = new InMemoryRefreshTokenRepository();
        FakePasswordHasher hasher = new FakePasswordHasher();
        MutableClock clock = new MutableClock(Instant.parse("2026-10-02T20:00:00Z"));
        SessionTokenService sessions = new SessionTokenService(
                refreshTokens, new FakeAccessTokenIssuer(), new OpaqueTokenGenerator(), clock, Duration.ofDays(7));
        AuthController controller = new AuthController(
                new RegisterUserService(users, hasher, clock),
                new LoginService(users, hasher, sessions),
                new TokenRefreshService(refreshTokens, users, sessions, clock));
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

    private String loginYObtenerRefresh() throws Exception {
        enviar("/api/v1/auth/register", registro("mj@watson.com", "Mary Jane", "Telarana2026"));
        String body = enviar("/api/v1/auth/login", credenciales("mj@watson.com", "Telarana2026"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.refreshToken");
    }

    // ------------------------------------------------------------------
    // Registro
    // ------------------------------------------------------------------
    @Test
    void registraYDevuelve201SinElHash() throws Exception {
        enviar("/api/v1/auth/register", registro("Peter@DailyBugle.com", "Peter Parker", "Telarana2026"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("peter@dailybugle.com"))
                .andExpect(jsonPath("$.displayName").value("Peter Parker"))
                .andExpect(jsonPath("$.role").value("LECTOR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("Telarana2026"))));
    }

    @Test
    void unCorreoRepetidoDevuelve409() throws Exception {
        String body = registro("mj@watson.com", "Mary Jane", "Telarana2026");
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
    void elLoginDevuelveElParDeTokens() throws Exception {
        enviar("/api/v1/auth/register", registro("mj@watson.com", "Mary Jane", "Telarana2026"));

        enviar("/api/v1/auth/login", credenciales("mj@watson.com", "Telarana2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void unLoginFallidoDevuelve401Generico() throws Exception {
        enviar("/api/v1/auth/register", registro("mj@watson.com", "Mary Jane", "Telarana2026"));

        enviar("/api/v1/auth/login", credenciales("mj@watson.com", "ClaveEquivocada1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.invalid_credentials"));
        enviar("/api/v1/auth/login", credenciales("nadie@watson.com", "Telarana2026"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.invalid_credentials"));
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
}