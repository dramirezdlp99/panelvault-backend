package com.panelvault.backend.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TwoFactorSettingsTest {

    private static final Instant AHORA = Instant.parse("2026-10-02T20:00:00Z");

    private TwoFactorSettings pendiente() {
        return TwoFactorSettings.pending(UserId.newId(), "cifrado", AHORA);
    }

    private TwoFactorSettings activa() {
        TwoFactorSettings settings = pendiente();
        settings.enable(List.of("fp1", "fp2", "fp3"), 100L, AHORA);
        return settings;
    }

    @Test
    void unaActivacionPendienteNoEstaActiva() {
        assertThat(pendiente().isEnabled()).isFalse();
        assertThat(pendiente().remainingRecoveryCodes()).isZero();
    }

    @Test
    void activarGuardaLosCodigosYElIntervaloConfirmado() {
        TwoFactorSettings settings = activa();

        assertThat(settings.isEnabled()).isTrue();
        assertThat(settings.enabledAt()).isEqualTo(AHORA);
        assertThat(settings.lastUsedTimeStep()).isEqualTo(100L);
        assertThat(settings.remainingRecoveryCodes()).isEqualTo(3);
    }

    @Test
    void noSePuedeActivarDosVecesNiSinCodigos() {
        assertThatThrownBy(() -> activa().enable(List.of("x"), 200L, AHORA)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> pendiente().enable(List.of(), 1L, AHORA)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unIntervaloSoloSeAceptaSiEsPosteriorAlUltimoUsado() {
        TwoFactorSettings settings = activa();

        assertThat(settings.acceptTimeStep(100L)).isFalse();
        assertThat(settings.acceptTimeStep(99L)).isFalse();
        assertThat(settings.acceptTimeStep(101L)).isTrue();
        assertThat(settings.acceptTimeStep(101L)).isFalse();
    }

    @Test
    void cadaCodigoDeRecuperacionSirveUnaSolaVez() {
        TwoFactorSettings settings = activa();

        assertThat(settings.useRecoveryCode("fp2")).isTrue();
        assertThat(settings.useRecoveryCode("fp2")).isFalse();
        assertThat(settings.useRecoveryCode("inexistente")).isFalse();
        assertThat(settings.remainingRecoveryCodes()).isEqualTo(2);
    }
}