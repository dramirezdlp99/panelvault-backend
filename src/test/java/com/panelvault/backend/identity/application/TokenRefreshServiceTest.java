package com.panelvault.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.panelvault.backend.identity.domain.RefreshToken;
import com.panelvault.backend.identity.domain.Role;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenRefreshServiceTest {

    private static final String CORREO = "peter@dailybugle.com";

    private AuthFixture f;
    private User peter;

    @BeforeEach
    void setUp() {
        f = new AuthFixture();
        peter = f.registrar(CORREO);
    }

    private AuthTokens iniciarSesion() {
        return f.iniciarSesion(CORREO);
    }

    private RefreshToken guardado(String rawToken) {
        String hash = OpaqueTokenGenerator.sha256Hex(rawToken);
        return f.refreshTokens.all().stream().filter(t -> t.tokenHash().equals(hash)).findFirst().orElseThrow();
    }

    private String codigo(Runnable accion) {
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
        f.clock.advance(Duration.ofMinutes(20));

        AuthTokens segundo = f.refresh.refresh(primero.refreshToken());

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
        AuthTokens segundo = f.refresh.refresh(primero.refreshToken());

        // Alguien presenta otra vez el primer token: se asume robo.
        assertThat(codigo(() -> f.refresh.refresh(primero.refreshToken()))).isEqualTo("auth.refresh_token_reused");

        // El token legitimo mas reciente tambien quedo revocado.
        assertThat(guardado(segundo.refreshToken()).isRevoked()).isTrue();
        assertThat(codigo(() -> f.refresh.refresh(segundo.refreshToken()))).isEqualTo("auth.refresh_token_reused");
    }

    @Test
    void elReusoNoAfectaOtrasSesionesDelMismoUsuario() {
        AuthTokens celular = iniciarSesion();
        AuthTokens portatil = iniciarSesion();
        f.refresh.refresh(celular.refreshToken());
        codigo(() -> f.refresh.refresh(celular.refreshToken()));

        assertThat(guardado(portatil.refreshToken()).isRevoked()).isFalse();
        assertThat(f.refresh.refresh(portatil.refreshToken()).refreshToken()).isNotBlank();
    }

    @Test
    void unTokenVencidoSeRechaza() {
        AuthTokens tokens = iniciarSesion();
        f.clock.advance(Duration.ofDays(7));

        assertThat(codigo(() -> f.refresh.refresh(tokens.refreshToken()))).isEqualTo("auth.refresh_token_expired");
    }

    @Test
    void unTokenDesconocidoOVacioSeRechaza() {
        assertThat(codigo(() -> f.refresh.refresh("token-inventado"))).isEqualTo("auth.refresh_token_invalid");
        assertThat(codigo(() -> f.refresh.refresh("  "))).isEqualTo("auth.refresh_token_invalid");
    }

    @Test
    void elNuevoAccessTokenReflejaElRolActual() {
        AuthTokens tokens = iniciarSesion();
        peter.changeRole(Role.CURADOR, f.clock.instant());
        f.users.save(peter);

        assertThat(f.refresh.refresh(tokens.refreshToken()).accessToken()).contains(":CURADOR:");
    }

    @Test
    void cerrarSesionRevocaLaFamiliaYEsIdempotente() {
        AuthTokens tokens = iniciarSesion();

        f.refresh.logout(tokens.refreshToken());
        f.refresh.logout(tokens.refreshToken());
        f.refresh.logout("token-inventado");
        f.refresh.logout(null);

        assertThat(guardado(tokens.refreshToken()).isRevoked()).isTrue();
        assertThat(codigo(() -> f.refresh.refresh(tokens.refreshToken()))).isEqualTo("auth.refresh_token_reused");
    }
}