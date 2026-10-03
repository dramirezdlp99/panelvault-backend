package com.panelvault.backend.reading.domain;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Un cambio de progreso enviado por un dispositivo (Value Object).
 *
 * @param currentPage     pagina actual (desde 1)
 * @param currentPanel    vineta actual dentro de la pagina en Lectura Guiada (desde 0)
 * @param guidedMode      si el usuario lee en modo Lectura Guiada
 * @param clientUpdatedAt cuando ocurrio el cambio segun el reloj del dispositivo
 * @param deviceId        identificador del dispositivo o pestana que lo envia
 */
public record ProgressUpdate(
        int currentPage, int currentPanel, boolean guidedMode, Instant clientUpdatedAt, String deviceId) {

    public static final int MAX_PANEL = 500;
    private static final Pattern DEVICE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    public ProgressUpdate {
        if (currentPage < 1) {
            throw new InvalidInputException("reading.invalid_page", "La pagina debe ser 1 o mayor");
        }
        if (currentPanel < 0 || currentPanel > MAX_PANEL) {
            throw new InvalidInputException("reading.invalid_panel", "La vineta debe estar entre 0 y " + MAX_PANEL);
        }
        Objects.requireNonNull(clientUpdatedAt, "La fecha del cambio es obligatoria");
        if (deviceId == null || !DEVICE_ID.matcher(deviceId).matches()) {
            throw new InvalidInputException(
                    "reading.invalid_device", "El id del dispositivo debe tener de 1 a 64 letras, numeros, - o _");
        }
    }

    /**
     * Un dispositivo con el reloj adelantado (por ejemplo, un ano en el futuro) ganaria siempre la
     * comparacion y ningun otro cambio volveria a aplicarse. Por eso la fecha se limita a "ahora"
     * segun el servidor.
     */
    public ProgressUpdate clampedTo(Instant serverNow) {
        if (clientUpdatedAt.isAfter(serverNow)) {
            return new ProgressUpdate(currentPage, currentPanel, guidedMode, serverNow, deviceId);
        }
        return this;
    }
}
