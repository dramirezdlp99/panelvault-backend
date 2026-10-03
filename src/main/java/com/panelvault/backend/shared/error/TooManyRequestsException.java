package com.panelvault.backend.shared.error;

/**
 * Se superaron los intentos permitidos en una ventana de tiempo. Se responde con 429 y la cabecera
 * {@code Retry-After}, que le dice al cliente cuantos segundos esperar.
 */
public class TooManyRequestsException extends DomainException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String code, String message, long retryAfterSeconds) {
        super(ErrorCategory.RATE_LIMITED, code, message);
        if (retryAfterSeconds < 1) {
            throw new IllegalArgumentException("El tiempo de espera debe ser de al menos 1 segundo");
        }
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}