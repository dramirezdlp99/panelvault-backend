package com.panelvault.backend.identity.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.panelvault.backend.identity.application.FakePasswordHasher;
import com.panelvault.backend.identity.application.InMemoryUserRepository;
import com.panelvault.backend.identity.application.RegisterUserService;
import com.panelvault.backend.shared.web.GlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Prueba del endpoint de registro de punta a punta, sin base de datos: controlador real, caso de
 * uso real y manejador de errores real, con el repositorio y el hasher en memoria.
 */
class AuthControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        RegisterUserService service = new RegisterUserService(
                new InMemoryUserRepository(),
                new FakePasswordHasher(),
                Clock.fixed(Instant.parse("2026-10-02T20:00:00Z"), ZoneOffset.UTC));
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static String cuerpo(String email, String displayName, String password) {
        return """
                {"email": "%s", "displayName": "%s", "password": "%s"}
                """.formatted(email, displayName, password);
    }

    @Test
    void registraYDevuelve201SinElHash() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Peter@DailyBugle.com", "Peter Parker", "Telarana2026")))
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
        String body = cuerpo("mj@watson.com", "Mary Jane", "Telarana2026");
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("user.email_taken"));
    }

    @Test
    void unaContrasenaDebilDevuelve400ConCodigoPropio() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("gwen@stacy.com", "Gwen", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("user.weak_password"));
    }

    @Test
    void camposVaciosDevuelven400ConLaListaDeCampos() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", "", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
    }
}