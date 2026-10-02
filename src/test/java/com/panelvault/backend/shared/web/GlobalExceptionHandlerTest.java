package com.panelvault.backend.shared.web;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.panelvault.backend.shared.error.BusinessRuleException;
import com.panelvault.backend.shared.error.ConflictException;
import com.panelvault.backend.shared.error.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prueba el manejador de errores con un controlador falso, sin arrancar la aplicacion ni usar
 * la base de datos: MockMvc simula las peticiones HTTP en memoria.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void recursoInexistenteResponde404ConSuCodigo() throws Exception {
        mvc.perform(get("/prueba/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("comic.not_found"))
                .andExpect(jsonPath("$.message").value("El comic no existe"))
                .andExpect(jsonPath("$.path").value("/prueba/no-existe"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void conflictoResponde409() throws Exception {
        mvc.perform(get("/prueba/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("user.email_taken"));
    }

    @Test
    void reglaDeNegocioResponde422() throws Exception {
        mvc.perform(get("/prueba/regla"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("reading.page_out_of_range"));
    }

    @Test
    void errorInesperadoNoFiltraDetallesInternosYDaUnaReferencia() throws Exception {
        mvc.perform(get("/prueba/explota"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("server.unexpected_error"))
                .andExpect(jsonPath("$.message").value(not(containsString("secreto"))))
                .andExpect(jsonPath("$.reference").isNotEmpty());
    }

    @Test
    void validacionFallidaListaCadaCampo() throws Exception {
        mvc.perform(post("/prueba/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\", \"correo\": \"no-es-un-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.validation_failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("nombre", "correo")));
    }

    @Test
    void jsonMalFormadoResponde400EnElFormatoUniforme() throws Exception {
        mvc.perform(post("/prueba/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.invalid"));
    }

    @Test
    void metodoNoPermitidoResponde405EnElFormatoUniforme() throws Exception {
        mvc.perform(post("/prueba/no-existe"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("request.method_not_allowed"));
    }

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/no-existe")
        String noExiste() {
            throw new NotFoundException("comic.not_found", "El comic no existe");
        }

        @GetMapping("/prueba/conflicto")
        String conflicto() {
            throw new ConflictException("user.email_taken", "Ese correo ya esta registrado");
        }

        @GetMapping("/prueba/regla")
        String regla() {
            throw new BusinessRuleException("reading.page_out_of_range", "La pagina no existe en el tomo");
        }

        @GetMapping("/prueba/explota")
        String explota() {
            throw new IllegalStateException("detalle interno secreto de la base de datos");
        }

        @PostMapping("/prueba/validar")
        String validar(@Valid @RequestBody Registro registro) {
            return "ok";
        }
    }

    record Registro(@NotBlank String nombre, @NotBlank @Email String correo) {}
}