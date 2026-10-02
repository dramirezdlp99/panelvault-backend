package com.panelvault.backend.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.shared.error.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IdentityValueObjectsTest {

    // ------------------------------------------------------------------
    // Email
    // ------------------------------------------------------------------
    @Test
    void elCorreoSeNormalizaSinEspaciosYEnMinusculas() {
        assertThat(new Email("  Peter.Parker@DailyBugle.com ").value())
                .isEqualTo("peter.parker@dailybugle.com");
    }

    @Test
    void dosCorreosQueSoloDifierenEnMayusculasSonIguales() {
        assertThat(new Email("Ana@Mail.com")).isEqualTo(new Email("ana@mail.com"));
    }

    @Test
    void laParteLocalEsLoAnteriorALaArroba() {
        assertThat(new Email("peter.parker@dailybugle.com").localPart()).isEqualTo("peter.parker");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "sin-arroba", "dos@@arrobas.com", "@sinusuario.com", "ana@sindominio",
            "ana@dominio.c", "con espacio@mail.com"})
    void rechazaCorreosMalFormados(String raw) {
        assertThatThrownBy(() -> new Email(raw))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("user.email_invalid");
    }

    @Test
    void rechazaCorreoNulo() {
        assertThatThrownBy(() -> new Email(null)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void rechazaCorreosDeMasDe254Caracteres() {
        String largo = "a".repeat(250) + "@x.co";
        assertThatThrownBy(() -> new Email(largo)).isInstanceOf(InvalidInputException.class);
    }

    // ------------------------------------------------------------------
    // DisplayName
    // ------------------------------------------------------------------
    @Test
    void elNombreSeRecortaYColapsaEspacios() {
        assertThat(new DisplayName("  Peter    Parker  ").value()).isEqualTo("Peter Parker");
    }

    @Test
    void elNombreAceptaTildesYEnes() {
        assertThat(new DisplayName("Ramírez De La Parra").value()).isEqualTo("Ramírez De La Parra");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "a", "linea\nnueva", "con\ttabulador"})
    void rechazaNombresInvalidos(String raw) {
        assertThatThrownBy(() -> new DisplayName(raw))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("user.display_name_invalid");
    }

    @Test
    void rechazaNombresDeMasDe40Caracteres() {
        assertThatThrownBy(() -> new DisplayName("x".repeat(41))).isInstanceOf(InvalidInputException.class);
    }
}