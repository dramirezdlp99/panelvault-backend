package com.panelvault.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.panelvault.backend.identity.domain.RefreshToken;
import com.panelvault.backend.identity.domain.Role;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenRefreshServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-02T20:00:00Z");

    private InMemoryUserRepository users;
    private InMemoryRefreshTokenRepository refreshTokens;
    private MutableClock clock;
    private LoginService login;
    private TokenRefreshService refresh;
    private User peter;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        refreshTokens = new InMemoryRefreshTokenRepository();
        clock = new MutableClock(AHORA);
        FakePasswordHasher hasher = new FakePasswordHasher();
        SessionTokenService sessions = new SessionTokenService(
                refreshTokens, new FakeAccessTokenIssuer(), new OpaqueTokenGenerator(), clock, Duration.ofDays(7));
        peter = new RegisterUserService(users, hasher, clock)
                .register(new RegisterUserCommand("peter@dailybugle.com", "Peter Parker", "Telarana2026"));
        login = new LoginService(users, hasher, sessions);
        refresh = new TokenRefreshService(refreshTokens, users, sessions, clock);
    }

    private AuthTokens iniciarSesion() {
        return login.login("peter@dailybugle.com", "Telarana2026");
    }

    private RefreshToken guardado(String rawToken) {
        String hash = OpaqueTokenGenerator.sha256Hex(rawToken);
        return refreshTokens.all().stream().filter(t -> t.tokenHash().equals(hash)).findFirst().orElseThrow();
    }

    private static String codigo(Runnable accion) {
        try {
            accion.run();
            return "sin error";
        } catch (UnauthenticatedException e) {
            return e.code();
        }
    }

    @Test
    void refrescarEntregaUnParNuevoYRevocaElAnterior() {
        AuthTokens primero = iniciarSesion();
        clock.advance(Duration.ofMinutes(20));

        AuthTokens segundo = refresh.refresh(primero.refreshToken());

        assertThat(segundo.refreshToken()).isNotEqualTo(primero.refreshToken());
        assertThat(segundo.accessToken()).isNotEqualTo(primero.accessToken());
        RefreshToken viejo = guardado(primero.refreshToken());
        RefreshToken nuevo = guardado(segundo.refreshToken());
        assertThat(viejo.isRevoked()).isTrue();
        assertThat(viejo.replacedBy()).isEqualTo(nuevo.id());
        assertThat(nuevo.familyId()).isEqualTo(viejo.familyId());
        assertThat(nuevo.isRevoked()).isFalse();
    }

    @Test
    void reusarUnTokenYaUsadoRevocaTodaLaFamilia() {
        AuthTokens primero = iniciarSesion();
        AuthTokens segundo = refresh.refresh(primero.refreshToken());

        // Alguien presenta otra vez el primer token: se asume robo.
        assertThat(codigo(() -> refresh.refresh(primero.refreshToken()))).isEqualTo("auth.refresh_token_reused");

        // El token legitimo mas reciente tambien quedo revocado.
        assertThat(guardado(segundo.refreshToken()).isRevoked()).isTrue();
        assertThat(codigo(() -> refresh.refresh(segundo.refreshToken()))).isEqualTo("auth.refresh_token_reused");
    }

    @Test
    void elReusoNoAfectaOtrasSesionesDelMismoUsuario() {
        AuthTokens celular = iniciarSesion();
        AuthTokens portatil = iniciarSesion();
        refresh.refresh(celular.refreshToken());
        codigo(() -> refresh.refresh(celular.refreshToken()));

        assertThat(guardado(portatil.refreshToken()).isRevoked()).isFalse();
        assertThat(refresh.refresh(portatil.refreshToken()).refreshToken()).isNotBlank();
    }

    @Test
    void unTokenVencidoSeRechaza() {
        AuthTokens tokens = iniciarSesion();
        clock.advance(Duration.ofDays(7));

        assertThat(codigo(() -> refresh.refresh(tokens.refreshToken()))).isEqualTo("auth.refresh_token_expired");
    }

    @Test
    void unTokenDesconocidoOVacioSeRechaza() {
        assertThat(codigo(() -> refresh.refresh("token-inventado"))).isEqualTo("auth.refresh_token_invalid");
        assertThat(codigo(() -> refresh.refresh("  "))).isEqualTo("auth.refresh_token_invalid");
    }

    @Test
    void elNuevoAccessTokenReflejaElRolActual() {
        AuthTokens tokens = iniciarSesion();
        peter.changeRole(Role.CURADOR, clock.instant());
        users.save(peter);

        assertThat(refresh.refresh(tokens.refreshToken()).accessToken()).contains(":CURADOR:");
    }

    @Test
    void cerrarSesionRevocaLaFamiliaYEsIdempotente() {
        AuthTokens tokens = iniciarSesion();

        refresh.logout(tokens.refreshToken());
        refresh.logout(tokens.refreshToken());
        refresh.logout("token-inventado");
        refresh.logout(null);

        assertThat(guardado(tokens.refreshToken()).isRevoked()).isTrue();
        assertThat(codigo(() -> refresh.refresh(tokens.refreshToken()))).isEqualTo("auth.refresh_token_reused");
    }
}