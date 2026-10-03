package com.panelvault.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.RefreshToken;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-02T20:00:00Z");

    private InMemoryUserRepository users;
    private InMemoryRefreshTokenRepository refreshTokens;
    private FakePasswordHasher hasher;
    private LoginService login;
    private User peter;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        refreshTokens = new InMemoryRefreshTokenRepository();
        hasher = new FakePasswordHasher();
        MutableClock clock = new MutableClock(AHORA);
        SessionTokenService sessions = new SessionTokenService(
                refreshTokens, new FakeAccessTokenIssuer(), new OpaqueTokenGenerator(), clock, Duration.ofDays(7));
        peter = new RegisterUserService(users, hasher, clock)
                .register(new RegisterUserCommand("peter@dailybugle.com", "Peter Parker", "Telarana2026"));
        login = new LoginService(users, hasher, sessions);
    }

    @Test
    void conCredencialesCorrectasEntregaElParDeTokens() {
        AuthTokens tokens = login.login("Peter@DailyBugle.com", "Telarana2026");

        assertThat(tokens.accessToken()).startsWith("access:" + peter.id() + ":LECTOR");
        assertThat(tokens.accessTokenExpiresInSeconds()).isEqualTo(15 * 60);
        assertThat(tokens.refreshToken()).hasSize(43);
        assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(AHORA.plus(Duration.ofDays(7)));
    }

    @Test
    void enLaBaseSoloQuedaElHashDelRefreshToken() {
        AuthTokens tokens = login.login("peter@dailybugle.com", "Telarana2026");

        RefreshToken guardado = refreshTokens.all().getFirst();
        assertThat(guardado.tokenHash()).isEqualTo(OpaqueTokenGenerator.sha256Hex(tokens.refreshToken()));
        assertThat(guardado.tokenHash()).isNotEqualTo(tokens.refreshToken());
    }

    @Test
    void cadaLoginAbreUnaFamiliaNueva() {
        login.login("peter@dailybugle.com", "Telarana2026");
        login.login("peter@dailybugle.com", "Telarana2026");

        assertThat(refreshTokens.all()).extracting(RefreshToken::familyId).doesNotHaveDuplicates();
    }

    @Test
    void unaContrasenaIncorrectaDaElMensajeGenerico() {
        assertThatThrownBy(() -> login.login("peter@dailybugle.com", "OtraClave2026"))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("Correo o contrasena incorrectos")
                .extracting("code")
                .isEqualTo("auth.invalid_credentials");
    }

    @Test
    void unCorreoInexistenteDaExactamenteElMismoErrorYTambienComparaUnaContrasena() {
        int antes = hasher.matchesCalls();

        assertThatThrownBy(() -> login.login("nadie@dailybugle.com", "Telarana2026"))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("Correo o contrasena incorrectos")
                .extracting("code")
                .isEqualTo("auth.invalid_credentials");
        // Se comparo contra el hash de relleno: el tiempo de respuesta no delata que no existe.
        assertThat(hasher.matchesCalls()).isEqualTo(antes + 1);
    }

    @Test
    void unCorreoMalFormadoTambienDaElErrorGenerico() {
        assertThatThrownBy(() -> login.login("esto-no-es-correo", "Telarana2026"))
                .isInstanceOf(UnauthenticatedException.class)
                .extracting("code")
                .isEqualTo("auth.invalid_credentials");
    }

    @Test
    void unLoginFallidoNoCreaSesiones() {
        assertThatThrownBy(() -> login.login("peter@dailybugle.com", "mal"))
                .isInstanceOf(UnauthenticatedException.class);
        assertThat(refreshTokens.all()).isEmpty();
    }
}