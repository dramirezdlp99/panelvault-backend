package com.panelvault.backend.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final Instant AHORA = Instant.parse("2026-10-02T20:00:00Z");
    private static final Duration SIETE_DIAS = Duration.ofDays(7);
    private final UserId usuario = UserId.newId();
    private final UUID familia = UUID.randomUUID();

    private RefreshToken nuevo(String hash) {
        return RefreshToken.issue(usuario, familia, hash, AHORA, SIETE_DIAS);
    }

    @Test
    void venceSegunSuDuracion() {
        assertThat(nuevo("h1").expiresAt()).isEqualTo(AHORA.plus(SIETE_DIAS));
    }

    @Test
    void estaVencidoDesdeElInstanteExactoDeVencimiento() {
        RefreshToken token = nuevo("h1");
        assertThat(token.isExpired(AHORA.plus(SIETE_DIAS).minusNanos(1000))).isFalse();
        assertThat(token.isExpired(AHORA.plus(SIETE_DIAS))).isTrue();
    }

    @Test
    void rotarLoRevocaYEnlazaASuSucesor() {
        RefreshToken actual = nuevo("h1");
        RefreshToken sucesor = nuevo("h2");
        actual.rotateTo(sucesor, AHORA.plusSeconds(10));

        assertThat(actual.isRevoked()).isTrue();
        assertThat(actual.revokedAt()).isEqualTo(AHORA.plusSeconds(10));
        assertThat(actual.replacedBy()).isEqualTo(sucesor.id());
        assertThat(sucesor.isRevoked()).isFalse();
    }

    @Test
    void unTokenRevocadoNoPuedeRotarseOtraVez() {
        RefreshToken actual = nuevo("h1");
        actual.rotateTo(nuevo("h2"), AHORA);
        assertThatThrownBy(() -> actual.rotateTo(nuevo("h3"), AHORA)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void elSucesorDebeSerDeLaMismaFamilia() {
        RefreshToken otraFamilia = RefreshToken.issue(usuario, UUID.randomUUID(), "h2", AHORA, SIETE_DIAS);
        assertThatThrownBy(() -> nuevo("h1").rotateTo(otraFamilia, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaDuracionesNoPositivas() {
        assertThatThrownBy(() -> RefreshToken.issue(usuario, familia, "h", AHORA, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}