package com.panelvault.backend.library.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.panelvault.backend.library.domain.ComicSamples;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Prueba de punta a punta de biblioteca y lectura: seguridad real, PostgreSQL y JSON.
 *
 * <p>A diferencia de otras pruebas, esta NO corre dentro de una sola transaccion: cada peticion
 * confirma la suya, como en produccion. Asi se prueba de verdad el borrado en cascada de la base.
 * Al final se borran los usuarios creados, y con ellos (en cascada) todos sus datos.
 */
@SpringBootTest
@ActiveProfiles("test")
class LibraryReadingApiIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<String> correosCreados = new ArrayList<>();

    private MockMvc mvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        token = registrarYEntrar();
    }

    @AfterEach
    void limpiar() {
        correosCreados.forEach(correo -> jdbc.update("DELETE FROM users WHERE email = ?", correo));
    }

    private String registrarYEntrar() throws Exception {
        String correo = "lector-" + UUID.randomUUID().toString().substring(0, 8) + "@panelvault.test";
        correosCreados.add(correo);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"displayName\":\"Lector\",\"password\":\"Telarana2026\"}".formatted(correo)));
        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Telarana2026\"}".formatted(correo)))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(login, "$.accessToken");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder request, String json) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions pedir(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static String comic(String title, int pages, int seed) {
        return """
                {"title": "%s", "series": "Amazing Fantasy", "issueNumber": "15", "pageCount": %d,
                 "format": "CBZ", "fileSha256": "%s", "readingDirection": "LEFT_TO_RIGHT",
                 "tags": ["Marvel", "Clasicos"]}
                """.formatted(title, pages, ComicSamples.sha256(seed));
    }

    private static String progreso(int page, String when, String device) {
        return """
                {"currentPage": %d, "currentPanel": 1, "guidedMode": true,
                 "clientUpdatedAt": "%s", "deviceId": "%s"}
                """.formatted(page, when, device);
    }

    @Test
    void laBibliotecaExigeSesion() throws Exception {
        mvc.perform(get("/api/v1/library/comics")).andExpect(status().isUnauthorized());
    }

    @Test
    void crearConsultarActualizarYBorrarUnComic() throws Exception {
        String id = UUID.randomUUID().toString();

        enviar(put("/api/v1/library/comics/" + id), comic("Spider-Man", 24, 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.tags[0]").value("marvel"))
                .andExpect(jsonPath("$.version").value(1));
        enviar(put("/api/v1/library/comics/" + id), comic("Spider-Man", 24, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        enviar(put("/api/v1/library/comics/" + id), comic("The Amazing Spider-Man", 24, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        pedir(get("/api/v1/library/comics").param("q", "amazing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].title").value("The Amazing Spider-Man"));

        pedir(delete("/api/v1/library/comics/" + id)).andExpect(status().isNoContent());
        pedir(get("/api/v1/library/comics/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("library.comic_not_found"));
    }

    @Test
    void rechazaDatosInvalidosYArchivosDuplicados() throws Exception {
        enviar(put("/api/v1/library/comics/no-es-uuid"), comic("X", 10, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("library.invalid_comic_id"));
        enviar(put("/api/v1/library/comics/" + UUID.randomUUID()), comic("X", 0, 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.validation_failed"));

        enviar(put("/api/v1/library/comics/" + UUID.randomUUID()), comic("Original", 10, 3))
                .andExpect(status().isCreated());
        enviar(put("/api/v1/library/comics/" + UUID.randomUUID()), comic("Copia", 10, 3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("library.duplicate_file"));
    }

    @Test
    void progresoSincronizadoRecientesYResumen() throws Exception {
        String id = UUID.randomUUID().toString();
        enviar(put("/api/v1/library/comics/" + id), comic("Akira", 40, 4)).andExpect(status().isCreated());

        enviar(put("/api/v1/reading/comics/" + id + "/progress"), progreso(10, "2026-01-01T15:00:00Z", "portatil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true))
                .andExpect(jsonPath("$.progress.percent").value(25.0));
        // Un cambio mas viejo de otro dispositivo no pisa el progreso.
        enviar(put("/api/v1/reading/comics/" + id + "/progress"), progreso(2, "2026-01-01T14:00:00Z", "celular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(false))
                .andExpect(jsonPath("$.progress.currentPage").value(10));

        pedir(get("/api/v1/reading/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Akira"))
                .andExpect(jsonPath("$[0].progress.currentPage").value(10));
        pedir(get("/api/v1/library/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comics").value(1))
                .andExpect(jsonPath("$.totalPages").value(40))
                .andExpect(jsonPath("$.comicsStarted").value(1))
                .andExpect(jsonPath("$.comicsFinished").value(0));
    }

    @Test
    void marcadoresYBorradoEnCascada() throws Exception {
        String id = UUID.randomUUID().toString();
        String marcador = UUID.randomUUID().toString();
        enviar(put("/api/v1/library/comics/" + id), comic("Watchmen", 30, 5)).andExpect(status().isCreated());
        enviar(put("/api/v1/reading/comics/" + id + "/progress"), progreso(5, "2026-01-01T15:00:00Z", "portatil"))
                .andExpect(status().isOk());

        enviar(put("/api/v1/reading/comics/" + id + "/bookmarks/" + marcador), "{\"page\": 12, \"note\": \"El reloj\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.page").value(12));
        pedir(get("/api/v1/reading/comics/" + id + "/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].note").value("El reloj"));

        // Al borrar el comic se borran su progreso y sus marcadores.
        pedir(delete("/api/v1/library/comics/" + id)).andExpect(status().isNoContent());
        pedir(delete("/api/v1/reading/bookmarks/" + marcador))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("reading.bookmark_not_found"));
        pedir(get("/api/v1/library/stats")).andExpect(jsonPath("$.comicsStarted").value(0));
    }

    @Test
    void unUsuarioNoVeLaBibliotecaDeOtro() throws Exception {
        String id = UUID.randomUUID().toString();
        enviar(put("/api/v1/library/comics/" + id), comic("Privado", 10, 6)).andExpect(status().isCreated());

        token = registrarYEntrar();

        pedir(get("/api/v1/library/comics/" + id)).andExpect(status().isNotFound());
        pedir(get("/api/v1/library/comics")).andExpect(jsonPath("$.totalItems").value(0));
        enviar(put("/api/v1/reading/comics/" + id + "/progress"), progreso(1, "2026-01-01T15:00:00Z", "x"))
                .andExpect(status().isNotFound());
    }
}
