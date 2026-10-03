package com.panelvault.backend.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    private static final Duration BASE = Duration.ofSeconds(10);
    private static final Duration TOPE = Duration.ofMinutes(5);

    /** Con azar = 1 el jitter es maximo: la espera es exactamente la exponencial completa. */
    private final RetryPolicy maxima = new RetryPolicy(5, BASE, TOPE, () -> 0.999999999);
    /** Con azar = 0 la espera es la mitad fija. */
    private final RetryPolicy minima = new RetryPolicy(5, BASE, TOPE, () -> 0.0);

    @Test
    void laEsperaCreceExponencialmente() {
        assertThat(maxima.nextDelay(1)).hasValueSatisfying(d -> assertThat(d.toSeconds()).isEqualTo(9));
        assertThat(maxima.nextDelay(2)).hasValueSatisfying(d -> assertThat(d.toSeconds()).isEqualTo(19));
        assertThat(maxima.nextDelay(3)).hasValueSatisfying(d -> assertThat(d.toSeconds()).isEqualTo(39));
        assertThat(maxima.nextDelay(4)).hasValueSatisfying(d -> assertThat(d.toSeconds()).isEqualTo(79));
    }

    @Test
    void elJitterNuncaBajaDeLaMitad() {
        assertThat(minima.nextDelay(1)).contains(Duration.ofSeconds(5));
        assertThat(minima.nextDelay(3)).contains(Duration.ofSeconds(20));
    }

    @Test
    void laEsperaTieneTope() {
        RetryPolicy muchos = new RetryPolicy(50, BASE, TOPE, () -> 0.999999999);
        assertThat(muchos.nextDelay(40).orElseThrow()).isLessThanOrEqualTo(TOPE);
    }

    @Test
    void sinIntentosRestantesNoHayEspera() {
        assertThat(maxima.nextDelay(5)).isEqualTo(Optional.empty());
        assertThat(maxima.nextDelay(9)).isEmpty();
    }

    @Test
    void rechazaConfiguracionesInvalidas() {
        assertThatThrownBy(() -> new RetryPolicy(0, BASE, TOPE, () -> 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryPolicy(3, Duration.ZERO, TOPE, () -> 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryPolicy(3, TOPE, BASE, () -> 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
