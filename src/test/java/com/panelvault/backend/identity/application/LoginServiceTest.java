package com.panelvault.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.RefreshToken;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.shared.error.TooManyRequestsException;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginServiceTest {

    private static final String CORREO = "peter@dailybugle.com";

    private AuthFixture f;
    private User peter;

    @BeforeEach
    void setUp() {
        f = new AuthFixture();
        peter = f.registrar(CORREO);
    }

    private String codigoDeError(String correo, String clave) {
        try {
            f.login.login(correo, clave);
            return "sin error";
        } catch (UnauthenticatedException | TooManyRequestsException e) {
            return e.code();
        }
    }

    // ------------------------------------------------------------------
    // Login sin 2FA
    // ------------------------------------------------------------------
    @Test
    void conCredencialesCorrectasEntregaElParDeTokens() {
        AuthTokens tokens = f.iniciarSesion("Peter@DailyBugle.com");

        assertThat(tokens.accessToken()).startsWith("access:" + peter.id() + ":LECTOR");
        assertThat(tokens.accessTokenExpiresInSeconds()).isEqualTo(15 * 60);
        assertThat(tokens.refreshToken()).hasSize(43);
        assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(AuthFixture.START.plus(Duration.ofDays(7)));
    }

    @Test
    void enLaBaseSoloQuedaElHashDelRefreshToken() {
        AuthTokens tokens = f.iniciarSesion(CORREO);

        RefreshToken guardado = f.refreshTokens.all().getFirst();
        assertThat(guardado.tokenHash()).isEqualTo(OpaqueTokenGenerator.sha256Hex(tokens.refreshToken()));
        assertThat(guardado.tokenHash()).isNotEqualTo(tokens.refreshToken());
    }

    @Test
    void cadaLoginAbreUnaFamiliaNueva() {
        f.iniciarSesion(CORREO);
        f.iniciarSesion(CORREO);

        assertThat(f.refreshTokens.all()).extracting(RefreshToken::familyId).doesNotHaveDuplicates();
    }

    @Test
    void unaContrasenaIncorrectaDaElMensajeGenerico() {
        assertThatThrownBy(() -> f.login.login(CORREO, "OtraClave2026"))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("Correo o contrasena incorrectos")
                .extracting("code")
                .isEqualTo("auth.invalid_credentials");
    }

    @Test
    void unCorreoInexistenteDaExactamenteElMismoErrorYTambienComparaUnaContrasena() {
        int antes = f.hasher.matchesCalls();

        assertThatThrownBy(() -> f.login.login("nadie@dailybugle.com", AuthFixture.PASSWORD))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("Correo o contrasena incorrectos")
                .extracting("code")
                .isEqualTo("auth.invalid_credentials");
        // Se comparo contra el hash de relleno: el tiempo de respuesta no delata que no existe.
        assertThat(f.hasher.matchesCalls()).isEqualTo(antes + 1);
    }

    @Test
    void unCorreoMalFormadoTambienDaElErrorGenerico() {
        assertThat(codigoDeError("esto-no-es-correo", AuthFixture.PASSWORD)).isEqualTo("auth.invalid_credentials");
    }

    @Test
    void unLoginFallidoNoCreaSesiones() {
        codigoDeError(CORREO, "mal");
        assertThat(f.refreshTokens.all()).isEmpty();
    }

    // ------------------------------------------------------------------
    // Login con 2FA
    // ------------------------------------------------------------------
    @Test
    void conDosPasosActivoPideElCodigoEnVezDeEntregarTokens() {
        f.activarDosPasos(peter);

        LoginResult result = f.login.login(CORREO, AuthFixture.PASSWORD);

        assertThat(result).isInstanceOf(LoginResult.TwoFactorRequired.class);
        LoginResult.TwoFactorRequired required = (LoginResult.TwoFactorRequired) result;
        assertThat(required.challenge().token()).isEqualTo("challenge:" + peter.id());
        assertThat(f.refreshTokens.all()).isEmpty();
    }

    @Test
    void conDosPasosActivoUnaContrasenaIncorrectaSigueSiendoUnErrorGenerico() {
        f.activarDosPasos(peter);
        assertThat(codigoDeError(CORREO, "OtraClave2026")).isEqualTo("auth.invalid_credentials");
    }

    // ------------------------------------------------------------------
    // Limite de intentos
    // ------------------------------------------------------------------
    @Test
    void cincoFallosBloqueanElCorreoAunqueLuegoLaContrasenaSeaCorrecta() {
        for (int i = 0; i < 5; i++) {
            codigoDeError(CORREO, "mal");
        }

        assertThatThrownBy(() -> f.login.login(CORREO, AuthFixture.PASSWORD))
                .isInstanceOf(TooManyRequestsException.class)
                .extracting("code")
                .isEqualTo("auth.too_many_attempts");
    }

    @Test
    void elBloqueoNoDistingueMayusculasDelCorreo() {
        for (int i = 0; i < 5; i++) {
            codigoDeError(i % 2 == 0 ? CORREO : "PETER@dailybugle.com", "mal");
        }
        assertThat(codigoDeError("Peter@DailyBugle.com", AuthFixture.PASSWORD)).isEqualTo("auth.too_many_attempts");
    }

    @Test
    void elBloqueoTerminaCuandoPasaLaVentana() {
        for (int i = 0; i < 5; i++) {
            codigoDeError(CORREO, "mal");
        }
        f.clock.advance(Duration.ofMinutes(15));

        assertThat(f.iniciarSesion(CORREO).accessToken()).isNotBlank();
    }

    @Test
    void unLoginCorrectoReiniciaElContador() {
        for (int i = 0; i < 4; i++) {
            codigoDeError(CORREO, "mal");
        }
        f.iniciarSesion(CORREO);
        for (int i = 0; i < 4; i++) {
            codigoDeError(CORREO, "mal");
        }

        assertThat(f.iniciarSesion(CORREO).accessToken()).isNotBlank();
    }

    @Test
    void elBloqueoDeUnCorreoNoAfectaAOtros() {
        f.registrar("mj@watson.com");
        for (int i = 0; i < 5; i++) {
            codigoDeError(CORREO, "mal");
        }

        assertThat(f.iniciarSesion("mj@watson.com").accessToken()).isNotBlank();
    }
}