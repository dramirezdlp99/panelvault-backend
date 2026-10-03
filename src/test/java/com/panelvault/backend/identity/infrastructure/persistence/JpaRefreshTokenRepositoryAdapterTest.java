package com.panelvault.backend.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.panelvault.backend.identity.domain.DisplayName;
import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.RefreshToken;
import com.panelvault.backend.identity.domain.User;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Prueba de integracion de los refresh tokens contra PostgreSQL (base panelvault_test). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaRefreshTokenRepositoryAdapterTest {

    private static final Instant AHORA = Instant.parse("2026-10-02T20:00:00.123456Z");
    private static final Duration SIETE_DIAS = Duration.ofDays(7);

    @Autowired
    private JpaRefreshTokenRepositoryAdapter tokens;

    @Autowired
    private JpaUserRepositoryAdapter users;

    private User usuario;

    @BeforeEach
    void setUp() {
        usuario = users.save(User.register(
                new Email("tokens-" + UUID.randomUUID() + "@panelvault.test"),
                new DisplayName("Usuario de Prueba"),
                "$2a$12$hashdeprueba",
                AHORA));
    }

    private RefreshToken token(UUID familia, String hash) {
        return RefreshToken.issue(usuario.id(), familia, hash, AHORA, SIETE_DIAS);
    }

    private static String hash(String semilla) {
        return (semilla + "0".repeat(64)).substring(0, 64);
    }

    @Test
    void guardaYBuscaPorHashConTodosSusDatos() {
        UUID familia = UUID.randomUUID();
        RefreshToken original = tokens.save(token(familia, hash("a1")));

        RefreshToken leido = tokens.findByTokenHashForUpdate(hash("a1")).orElseThrow();

        assertThat(leido.id()).isEqualTo(original.id());
        assertThat(leido.userId()).isEqualTo(usuario.id());
        assertThat(leido.familyId()).isEqualTo(familia);
        assertThat(leido.issuedAt()).isEqualTo(AHORA);
        assertThat(leido.expiresAt()).isEqualTo(AHORA.plus(SIETE_DIAS));
        assertThat(leido.isRevoked()).isFalse();
    }

    @Test
    void unHashDesconocidoNoEncuentraNada() {
        assertThat(tokens.findByTokenHashForUpdate(hash("ffff"))).isEmpty();
    }

    @Test
    void guardaLaRotacion() {
        UUID familia = UUID.randomUUID();
        RefreshToken primero = tokens.save(token(familia, hash("b1")));
        RefreshToken segundo = token(familia, hash("b2"));
        primero.rotateTo(segundo, AHORA.plusSeconds(30));
        tokens.save(segundo);
        tokens.save(primero);

        RefreshToken leido = tokens.findByTokenHashForUpdate(hash("b1")).orElseThrow();
        assertThat(leido.isRevoked()).isTrue();
        assertThat(leido.replacedBy()).isEqualTo(segundo.id());
    }

    @Test
    void revocarUnaFamiliaSoloAfectaSusTokensActivos() {
        UUID familiaA = UUID.randomUUID();
        UUID familiaB = UUID.randomUUID();
        RefreshToken a1 = tokens.save(token(familiaA, hash("c1")));
        RefreshToken a2 = token(familiaA, hash("c2"));
        a1.rotateTo(a2, AHORA.plusSeconds(10));
        tokens.save(a2);
        tokens.save(a1);
        tokens.save(token(familiaB, hash("c3")));

        int revocados = tokens.revokeFamily(familiaA, AHORA.plusSeconds(60));

        assertThat(revocados).isEqualTo(1);
        assertThat(tokens.findByTokenHashForUpdate(hash("c2")).orElseThrow().isRevoked()).isTrue();
        assertThat(tokens.findByTokenHashForUpdate(hash("c3")).orElseThrow().isRevoked()).isFalse();
        // a1 conserva la fecha de su rotacion original; el UPDATE no la sobrescribe.
        assertThat(tokens.findByTokenHashForUpdate(hash("c1")).orElseThrow().revokedAt())
                .isEqualTo(AHORA.plusSeconds(10));
    }
}