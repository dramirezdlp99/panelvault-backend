package com.panelvault.backend.identity.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.Role;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserRepository;
import java.time.Clock;
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
 * JWT firmados de verdad y PostgreSQL. Cada prueba se deshace al terminar.
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
}