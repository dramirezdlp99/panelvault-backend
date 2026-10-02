package com.panelvault.backend.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.shared.error.InvalidInputException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();
    private final Email email = new Email("peter.parker@dailybugle.com");

    @Test
    void aceptaUnaContrasenaQueCumpleTodo() {
        assertThat(policy.violations("Telarana2026", email)).isEmpty();
    }

    @Test
    void exigeLongitudMinima() {
        assertThat(policy.violations("corta1", email)).containsExactly("debe tener al menos 10 caracteres");
    }

    @Test
    void exigeAlMenosUnaLetraYUnDigito() {
        assertThat(policy.violations("12345678901", email)).containsExactly("debe incluir al menos una letra");
        assertThat(policy.violations("solamenteletras", email))
                .containsExactly("debe incluir al menos un digito");
    }

    @Test
    void rechazaContrasenasQueSuperan72BytesAunqueTenganPocosCaracteres() {
        // 37 letras con tilde ocupan 74 bytes en UTF-8: pocos caracteres, demasiados bytes para BCrypt.
        String larga = "á".repeat(37) + "1";
        assertThat(larga.length()).isLessThan(72);
        assertThat(policy.violations(larga, email)).contains("es demasiado larga (maximo 72 bytes)");
    }

    @Test
    void rechazaContrasenasQueContienenElUsuarioDelCorreo() {
        assertThat(policy.violations("PETER.PARKER99", email)).containsExactly("no puede contener tu usuario de correo");
    }

    @Test
    void reportaTodasLasFallasJuntas() {
        assertThat(policy.violations("abc", email)).hasSize(2);
    }

    @Test
    void checkLanzaExcepcionConCodigoWeakPassword() {
        assertThatThrownBy(() -> policy.check("abc", email))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("user.weak_password");
    }

    @Test
    void unaContrasenaVaciaSoloReportaQueEsObligatoria() {
        assertThat(policy.violations("   ", email)).containsExactly("es obligatoria");
    }
}