package com.panelvault.backend.catalog.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Prueba de punta a punta del catalogo publico, la curaduria (RBAC) y CORS. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogApiIntegrationTest {

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

    private String usuarioConRol(Role role) throws Exception {
        String correo = "curaduria-" + UUID.randomUUID().toString().substring(0, 8) + "@panelvault.test";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"displayName\":\"Persona\",\"password\":\"Telarana2026\"}".formatted(correo)));
        if (role != Role.LECTOR) {
            User user = users.findByEmail(new Email(correo)).orElseThrow();
            user.changeRole(role, clock.instant());
            users.save(user);
        }
        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Telarana2026\"}".formatted(correo)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(login, "$.accessToken");
    }

    private ResultActions conToken(String token, MockHttpServletRequestBuilder request, String json) throws Exception {
        MockHttpServletRequestBuilder builder = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(builder);
    }

    private static String obra(String title, String sourceUrl) {
        return """
                {"title": "%s", "author": "Autor de Prueba", "year": 1920, "description": "Obra libre",
                 "sourceUrl": "%s", "license": "PUBLIC_DOMAIN", "tags": ["Prueba"]}
                """.formatted(title, sourceUrl);
    }

    // ------------------------------------------------------------------
    // Catalogo publico
    // ------------------------------------------------------------------
    @Test
    void elCatalogoEsPublicoYTraeLasObrasSembradas() throws Exception {
        mvc.perform(get("/api/v1/catalog/works"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("public")))
                .andExpect(jsonPath("$.items[*].slug", hasItem("little-nemo-in-slumberland")));

        mvc.perform(get("/api/v1/catalog/works/little-nemo-in-slumberland"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("Winsor McCay"))
                .andExpect(jsonPath("$.license").value("PUBLIC_DOMAIN"));

        mvc.perform(get("/api/v1/catalog/slugs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("krazy-kat")));

        mvc.perform(get("/api/v1/catalog/works").param("q", "herriman"))
                .andExpect(jsonPath("$.items[*].slug", hasItem("krazy-kat")));
    }

    @Test
    void unaObraInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/v1/catalog/works/no-existe-esta-obra"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("catalog.work_not_found"));
    }

    // ------------------------------------------------------------------
    // Curaduria (RBAC)
    // ------------------------------------------------------------------
    @Test
    void unLectorNoPuedeCurarNiSinSesionSePuede() throws Exception {
        mvc.perform(post("/api/v1/curation/works").contentType(MediaType.APPLICATION_JSON)
                        .content(obra("X", "https://a.org")))
                .andExpect(status().isUnauthorized());
        conToken(usuarioConRol(Role.LECTOR), post("/api/v1/curation/works"), obra("X", "https://a.org"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void cicloCompletoDeUnaObraConUnCurador() throws Exception {
        String token = usuarioConRol(Role.CURADOR);
        String titulo = "Obra de Prueba " + UUID.randomUUID().toString().substring(0, 6);

        String creada = conToken(token, post("/api/v1/curation/works"), obra(titulo, "https://example.org/obra"))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.published").value(false))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(creada, "$.id");
        String slug = JsonPath.read(creada, "$.slug");

        // Borrador: invisible al publico.
        mvc.perform(get("/api/v1/catalog/works/" + slug)).andExpect(status().isNotFound());

        conToken(token, post("/api/v1/curation/works/" + id + "/publish"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
        mvc.perform(get("/api/v1/catalog/works/" + slug)).andExpect(status().isOk());

        conToken(token, put("/api/v1/curation/works/" + id), obra(titulo + " (editada)", "https://example.org/obra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug));

        conToken(token, post("/api/v1/curation/works/" + id + "/unpublish"), null).andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalog/works/" + slug)).andExpect(status().isNotFound());

        conToken(token, delete("/api/v1/curation/works/" + id), null).andExpect(status().isNoContent());
        conToken(token, get("/api/v1/curation/works/" + id), null).andExpect(status().isNotFound());
    }

    @Test
    void unAdminTambienPuedeCurarPorLaJerarquiaDeRoles() throws Exception {
        conToken(usuarioConRol(Role.ADMIN), post("/api/v1/curation/works"),
                        obra("Obra del Admin " + UUID.randomUUID().toString().substring(0, 6), "https://example.org"))
                .andExpect(status().isCreated());
    }

    @Test
    void rechazaFuentesQueNoSonHttps() throws Exception {
        conToken(usuarioConRol(Role.CURADOR), post("/api/v1/curation/works"), obra("Insegura", "http://example.org"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("catalog.invalid_source_url"));
    }

    // ------------------------------------------------------------------
    // CORS
    // ------------------------------------------------------------------
    @Test
    void corsSoloAutorizaLosOrigenesConfigurados() throws Exception {
        mvc.perform(options("/api/v1/library/comics")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));

        mvc.perform(options("/api/v1/library/comics")
                        .header(HttpHeaders.ORIGIN, "https://sitio-malicioso.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
