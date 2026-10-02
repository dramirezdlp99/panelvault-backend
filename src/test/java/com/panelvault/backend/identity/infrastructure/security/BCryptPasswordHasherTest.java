package com.panelvault.backend.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void elHashNoContieneLaContrasenaYUsaFactor12() {
        String hash = hasher.hash("Telarana2026");
        assertThat(hash).doesNotContain("Telarana2026").startsWith("$2").contains("$12$").hasSize(60);
    }

    @Test
    void verificaLaContrasenaCorrectaYRechazaLaIncorrecta() {
        String hash = hasher.hash("Telarana2026");
        assertThat(hasher.matches("Telarana2026", hash)).isTrue();
        assertThat(hasher.matches("telarana2026", hash)).isFalse();
    }

    @Test
    void laMismaContrasenaProduceHashesDistintosPorLaSal() {
        assertThat(hasher.hash("Telarana2026")).isNotEqualTo(hasher.hash("Telarana2026"));
    }
}