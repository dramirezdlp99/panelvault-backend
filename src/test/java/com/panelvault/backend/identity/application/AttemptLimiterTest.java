package com.panelvault.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.shared.error.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AttemptLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-02T20:00:00Z"));
    private final AttemptLimiter limiter = new AttemptLimiter(5, Duration.ofMinutes(15), clock);

    private void fallar(String clave, int veces) {
        for (int i = 0; i < veces; i++) {
            limiter.recordFailure(clave);
        }
    }

    private long esperaEnSegundos(String clave) {
        try {
            limiter.ensureAllowed(clave);
            return 0;
        } catch (TooManyRequestsException e) {
            return e.retryAfterSeconds();
        }
    }

    @Test
    void permiteIntentosMientrasNoSeLlegueAlLimite() {
        fallar("login:ana", 4);
        limiter.ensureAllowed("login:ana");
    }

    @Test
    void alQuintoFalloBloqueaYDiceCuantoEsperar() {
        fallar("login:ana", 5);

        assertThatThrownBy(() -> limiter.ensureAllowed("login:ana"))
                .isInstanceOf(TooManyRequestsException.class)
                .extracting("code")
                .isEqualTo("auth.too_many_attempts");
        assertThat(esperaEnSegundos("login:ana")).isEqualTo(15 * 60);
    }

    @Test
    void elTiempoDeEsperaDisminuyeConElReloj() {
        fallar("login:ana", 5);
        clock.advance(Duration.ofMinutes(10));

        assertThat(esperaEnSegundos("login:ana")).isEqualTo(5 * 60);
    }

    @Test
    void laVentanaEsDeslizante() {
        // Un fallo por minuto: a los 4 minutos ya hay 5 en la ventana.
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("login:ana");
            clock.advance(Duration.ofMinutes(1));
        }
        assertThat(esperaEnSegundos("login:ana")).isPositive();

        // Cuando el primer fallo cumple 15 minutos sale de la ventana y quedan 4: se permite.
        clock.advance(Duration.ofMinutes(10).plusSeconds(1));
        assertThat(esperaEnSegundos("login:ana")).isZero();
    }

    @Test
    void unAciertoBorraElHistorial() {
        fallar("login:ana", 4);
        limiter.reset("login:ana");
        fallar("login:ana", 4);

        assertThat(esperaEnSegundos("login:ana")).isZero();
    }

    @Test
    void cadaClaveTieneSuPropioContador() {
        fallar("login:ana", 5);

        assertThat(esperaEnSegundos("login:ana")).isPositive();
        assertThat(esperaEnSegundos("login:beto")).isZero();
    }

    @Test
    void purgaLasClavesInactivasParaNoCrecerSinLimite() {
        for (int i = 0; i < AttemptLimiter.MAX_TRACKED_KEYS; i++) {
            limiter.recordFailure("login:usuario" + i);
        }
        clock.advance(Duration.ofMinutes(16));
        limiter.recordFailure("login:nuevo");

        assertThat(limiter.trackedKeys()).isEqualTo(1);
    }

    @Test
    void rechazaConfiguracionesInvalidas() {
        assertThatThrownBy(() -> new AttemptLimiter(0, Duration.ofMinutes(1), clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AttemptLimiter(5, Duration.ZERO, clock))
                .isInstanceOf(IllegalArgumentException.class);
    }
}