package com.panelvault.backend.identity.application;

import com.panelvault.backend.shared.error.TooManyRequestsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador de intentos fallidos con ventana deslizante (sliding window log).
 *
 * <p>Por cada clave (por ejemplo {@code login:ana@mail.com}) se guarda la hora de cada fallo. Si en
 * los ultimos {@code window} minutos hay {@code maxFailures} fallos, se bloquea hasta que el fallo
 * mas antiguo salga de la ventana. A diferencia de una ventana fija ("5 por cada cuarto de hora del
 * reloj"), no permite rafagas justo en el cambio de ventana.
 *
 * <p>Vive en memoria: es suficiente para una sola instancia (como en Render Free). Con varias
 * instancias habria que moverlo a un almacen compartido; la interfaz no cambiaria.
 */
public class AttemptLimiter {

    /** Limite de claves en memoria; al superarlo se purgan las inactivas para no crecer sin fin. */
    static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxFailures;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public AttemptLimiter(int maxFailures, Duration window, Clock clock) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("Debe permitirse al menos un intento");
        }
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("La ventana debe ser positiva");
        }
        this.maxFailures = maxFailures;
        this.window = window;
        this.clock = Objects.requireNonNull(clock);
    }

    /** Lanza {@link TooManyRequestsException} si la clave esta bloqueada en este momento. */
    public void ensureAllowed(String key) {
        Instant now = clock.instant();
        Deque<Instant> log = failures.get(key);
        if (log == null) {
            return;
        }
        Instant unlockAt = null;
        synchronized (log) {
            prune(log, now);
            if (log.size() >= maxFailures) {
                unlockAt = log.peekFirst().plus(window);
            }
        }
        if (unlockAt != null) {
            long seconds = Math.max(1, (long) Math.ceil(Duration.between(now, unlockAt).toMillis() / 1000.0));
            throw new TooManyRequestsException(
                    "auth.too_many_attempts",
                    "Demasiados intentos fallidos. Intenta de nuevo en " + minutesText(seconds),
                    seconds);
        }
    }

    public void recordFailure(String key) {
        Instant now = clock.instant();
        if (failures.size() >= MAX_TRACKED_KEYS) {
            purgeInactive(now);
        }
        Deque<Instant> log = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (log) {
            prune(log, now);
            log.addLast(now);
        }
    }

    /** Un acierto borra el historial de fallos de esa clave. */
    public void reset(String key) {
        failures.remove(key);
    }

    int trackedKeys() {
        return failures.size();
    }

    private void prune(Deque<Instant> log, Instant now) {
        Instant limit = now.minus(window);
        while (!log.isEmpty() && !log.peekFirst().isAfter(limit)) {
            log.pollFirst();
        }
    }

    private void purgeInactive(Instant now) {
        failures.entrySet().removeIf(entry -> {
            Deque<Instant> log = entry.getValue();
            synchronized (log) {
                prune(log, now);
                return log.isEmpty();
            }
        });
    }

    private static String minutesText(long seconds) {
        long minutes = (seconds + 59) / 60;
        return minutes <= 1 ? "1 minuto" : minutes + " minutos";
    }
}