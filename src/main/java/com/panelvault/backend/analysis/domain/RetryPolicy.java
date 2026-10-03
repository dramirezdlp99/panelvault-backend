package com.panelvault.backend.analysis.domain;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoubleSupplier;

/**
 * Politica de reintentos con espera exponencial y "jitter" (Domain Service).
 *
 * <p>Tras el intento {@code n} fallido se espera {@code base * 2^(n-1)}, con un tope. Asi, si el
 * motor de IA esta caido, no se le bombardea: 10 s, 20 s, 40 s, 80 s...
 *
 * <p>El jitter ("equal jitter") toma la mitad fija y la otra mitad al azar. Sin el, todos los
 * trabajos que fallaron juntos reintentarian en el mismo instante (manada estampida) y volverian
 * a tumbar al motor.
 */
public final class RetryPolicy {

    private final int maxAttempts;
    private final Duration baseDelay;
    private final Duration maxDelay;
    private final DoubleSupplier random;

    public RetryPolicy(int maxAttempts, Duration baseDelay, Duration maxDelay, DoubleSupplier random) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Debe permitirse al menos un intento");
        }
        if (baseDelay == null || baseDelay.isNegative() || baseDelay.isZero()) {
            throw new IllegalArgumentException("La espera base debe ser positiva");
        }
        if (maxDelay == null || maxDelay.compareTo(baseDelay) < 0) {
            throw new IllegalArgumentException("La espera maxima no puede ser menor que la base");
        }
        this.maxAttempts = maxAttempts;
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
        this.random = Objects.requireNonNull(random);
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    /**
     * Espera antes del siguiente intento, o vacio si ya se agotaron.
     *
     * @param attemptsMade intentos ya realizados (el que acaba de fallar incluido)
     */
    public Optional<Duration> nextDelay(int attemptsMade) {
        if (attemptsMade >= maxAttempts) {
            return Optional.empty();
        }
        long exponential = baseDelay.toMillis() << Math.min(Math.max(attemptsMade - 1, 0), 30);
        long capped = Math.min(exponential, maxDelay.toMillis());
        long half = capped / 2;
        long jitter = (long) (random.getAsDouble() * (capped - half));
        return Optional.of(Duration.ofMillis(half + jitter));
    }
}
