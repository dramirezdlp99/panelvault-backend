package com.panelvault.backend.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.panelvault.backend.identity.domain.DisplayName;
import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.TwoFactorSettings;
import com.panelvault.backend.identity.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Prueba de integracion de la configuracion 2FA contra PostgreSQL (base panelvault_test). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaTwoFactorRepositoryAdapterTest {

    private static final Instant AHORA = Instant.parse("2026-10-02T20:00:00.123456Z");

    @Autowired
    private JpaTwoFactorRepositoryAdapter twoFactor;

    @Autowired
    private JpaUserRepositoryAdapter users;

    private User usuario;

    @BeforeEach
    void setUp() {
        usuario = users.save(User.register(
                new Email("2fa-" + UUID.randomUUID() + "@panelvault.test"),
                new DisplayName("Usuario de Prueba"),
                "$2a$12$hashdeprueba",
                AHORA));
    }

    @Test
    void guardaUnaActivacionPendiente() {
        twoFactor.save(TwoFactorSettings.pending(usuario.id(), "secreto-cifrado", AHORA));

        TwoFactorSettings leida = twoFactor.findByUserId(usuario.id()).orElseThrow();
        assertThat(leida.isEnabled()).isFalse();
        assertThat(leida.encryptedSecret()).isEqualTo("secreto-cifrado");
        assertThat(leida.createdAt()).isEqualTo(AHORA);
        assertThat(leida.remainingRecoveryCodes()).isZero();
        assertThat(leida.lastUsedTimeStep()).isNull();
    }

    @Test
    void guardaLaActivacionConSusCodigosYElUltimoIntervalo() {
        TwoFactorSettings settings = TwoFactorSettings.pending(usuario.id(), "secreto-cifrado", AHORA);
        settings.enable(List.of("a".repeat(64), "b".repeat(64), "c".repeat(64)), 58_000_000L, AHORA.plusSeconds(30));
        twoFactor.save(settings);

        TwoFactorSettings leida = twoFactor.findByUserIdForUpdate(usuario.id()).orElseThrow();
        assertThat(leida.isEnabled()).isTrue();
        assertThat(leida.enabledAt()).isEqualTo(AHORA.plusSeconds(30));
        assertThat(leida.lastUsedTimeStep()).isEqualTo(58_000_000L);
        assertThat(leida.recoveryCodeFingerprints()).containsExactly("a".repeat(64), "b".repeat(64), "c".repeat(64));
    }

    @Test
    void cabenDiezHuellasDeCodigosDeRecuperacion() {
        TwoFactorSettings settings = TwoFactorSettings.pending(usuario.id(), "secreto-cifrado", AHORA);
        List<String> diez = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> String.valueOf(i).repeat(64))
                .toList();
        settings.enable(diez, 1L, AHORA);
        twoFactor.save(settings);

        assertThat(twoFactor.findByUserId(usuario.id()).orElseThrow().remainingRecoveryCodes()).isEqualTo(10);
    }

    @Test
    void eliminarLaDejaSinConfiguracion() {
        twoFactor.save(TwoFactorSettings.pending(usuario.id(), "secreto-cifrado", AHORA));

        twoFactor.delete(usuario.id());

        assertThat(twoFactor.findByUserId(usuario.id())).isEmpty();
    }
}