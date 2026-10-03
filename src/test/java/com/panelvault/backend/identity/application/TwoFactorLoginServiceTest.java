package com.panelvault.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.shared.crypto.Totp;
import com.panelvault.backend.shared.error.TooManyRequestsException;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TwoFactorLoginServiceTest {

    private static final String CORREO = "peter@dailybugle.com";

    private AuthFixture f;
    private User peter;
    private AuthFixture.Activation activation;

    @BeforeEach
    void setUp() {
        f = new AuthFixture();
        peter = f.registrar(CORREO);
        activation = f.activarDosPasos(peter);
    }

    private String ticket() {
        LoginResult result = f.login.login(CORREO, AuthFixture.PASSWORD);
        return ((LoginResult.TwoFactorRequired) result).challenge().token();
    }

    private String codigoDeError(String ticket, String code) {
        try {
            f.twoFactorLogin.verify(ticket, code);
            return "sin error";
        } catch (UnauthenticatedException | TooManyRequestsException e) {
            return e.code();
        }
    }

    @Test
    void conElCodigoDeLaAppSeAbreLaSesion() {
        AuthTokens tokens = f.twoFactorLogin.verify(ticket(), f.codigoSiguiente(activation.secret()));

        assertThat(tokens.accessToken()).startsWith("access:" + peter.id());
        assertThat(f.refreshTokens.all()).hasSize(1);
    }

    @Test
    void unCodigoYaUsadoNoSirveOtraVez() {
        String codigo = f.codigoSiguiente(activation.secret());
        f.twoFactorLogin.verify(ticket(), codigo);

        // Mismo codigo, dentro de sus mismos 30 segundos: alguien lo vio y lo intenta repetir.
        assertThat(codigoDeError(ticket(), codigo)).isEqualTo("auth.invalid_2fa_code");
    }

    @Test
    void elCodigoUsadoAlActivarTampocoSirveParaEntrar() {
        // El codigo con el que se confirmo la activacion no puede reutilizarse en el login.
        assertThat(codigoDeError(ticket(), f.codigoActual(activation.secret()))).isEqualTo("auth.invalid_2fa_code");
    }

    @Test
    void seToleraUnIntervaloDeDesfaseDeRelojPeroNoMas() {
        long activado = Totp.timeStep(f.clock.instant());

        // Reloj del servidor en activado + 2; la app del usuario va 30 s atrasada (activado + 1).
        f.clock.advance(Duration.ofSeconds(60));
        assertThat(codigoDeError(ticket(), Totp.code(activation.secret(), activado + 1))).isEqualTo("sin error");

        // Reloj en activado + 4; un codigo de activado + 2 ya tiene 60 s de atraso: demasiado viejo.
        f.clock.advance(Duration.ofSeconds(60));
        assertThat(codigoDeError(ticket(), Totp.code(activation.secret(), activado + 2)))
                .isEqualTo("auth.invalid_2fa_code");
    }

    @Test
    void unCodigoDeRecuperacionSirveUnaSolaVezYSeAceptaEnMinusculasSinGuion() {
        String recuperacion = activation.recoveryCodes().get(0);
        String comoLoEscribiriaAlguien = recuperacion.replace("-", "").toLowerCase(Locale.ROOT);

        assertThat(codigoDeError(ticket(), comoLoEscribiriaAlguien)).isEqualTo("sin error");
        assertThat(codigoDeError(ticket(), recuperacion)).isEqualTo("auth.invalid_2fa_code");
        assertThat(f.twoFactorSetup.status(peter.id()).recoveryCodesRemaining()).isEqualTo(9);
    }

    @Test
    void unTicketInventadoSeRechaza() {
        assertThat(codigoDeError("challenge:no-es-un-uuid", "123456")).isEqualTo("auth.challenge_invalid");
        assertThat(codigoDeError("cualquier-cosa", "123456")).isEqualTo("auth.challenge_invalid");
    }

    @Test
    void cincoCodigosIncorrectosBloqueanElSegundoPaso() {
        String ticket = ticket();
        for (int i = 0; i < 5; i++) {
            codigoDeError(ticket, "000000");
        }

        // Ni siquiera el codigo correcto entra mientras dure el bloqueo.
        assertThat(codigoDeError(ticket, f.codigoSiguiente(activation.secret()))).isEqualTo("auth.too_many_attempts");
    }
}