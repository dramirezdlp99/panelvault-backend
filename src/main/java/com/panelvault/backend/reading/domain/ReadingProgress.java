package com.panelvault.backend.reading.domain;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.shared.error.InvalidInputException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Progreso de lectura de un usuario en un comic (Entidad), sincronizado entre dispositivos.
 *
 * <p><b>Resolucion de conflictos: "el ultimo que escribe gana" (LWW, last-writer-wins).</b> Si el
 * celular estuvo offline y envia tarde un cambio viejo, no debe pisar lo que el usuario leyo
 * despues en el portatil. Se aplica solo el cambio con la fecha mas reciente; si dos fechas son
 * identicas, gana el id de dispositivo mayor, para que todos los dispositivos lleguen siempre al
 * mismo resultado sin importar el orden en que lleguen los mensajes.
 */
public final class ReadingProgress {

    private final UserId user;
    private final ComicId comic;
    private int currentPage;
    private int totalPages;
    private int currentPanel;
    private boolean guidedMode;
    private Instant clientUpdatedAt;
    private String deviceId;
    private Instant serverUpdatedAt;

    private ReadingProgress(
            UserId user,
            ComicId comic,
            int currentPage,
            int totalPages,
            int currentPanel,
            boolean guidedMode,
            Instant clientUpdatedAt,
            String deviceId,
            Instant serverUpdatedAt) {
        this.user = Objects.requireNonNull(user);
        this.comic = Objects.requireNonNull(comic);
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.currentPanel = currentPanel;
        this.guidedMode = guidedMode;
        this.clientUpdatedAt = truncate(Objects.requireNonNull(clientUpdatedAt));
        this.deviceId = Objects.requireNonNull(deviceId);
        this.serverUpdatedAt = truncate(Objects.requireNonNull(serverUpdatedAt));
        validatePage(currentPage, totalPages);
    }

    public static ReadingProgress start(
            UserId user, ComicId comic, int totalPages, ProgressUpdate update, Instant now) {
        return new ReadingProgress(user, comic, update.currentPage(), totalPages, update.currentPanel(),
                update.guidedMode(), update.clientUpdatedAt(), update.deviceId(), now);
    }

    public static ReadingProgress rehydrate(
            UserId user,
            ComicId comic,
            int currentPage,
            int totalPages,
            int currentPanel,
            boolean guidedMode,
            Instant clientUpdatedAt,
            String deviceId,
            Instant serverUpdatedAt) {
        return new ReadingProgress(user, comic, currentPage, totalPages, currentPanel, guidedMode, clientUpdatedAt,
                deviceId, serverUpdatedAt);
    }

    /** Indica si {@code update} es posterior a lo guardado segun la regla LWW. */
    public boolean isOlderThan(ProgressUpdate update) {
        Instant incoming = truncate(update.clientUpdatedAt());
        int byTime = incoming.compareTo(clientUpdatedAt);
        return byTime > 0 || (byTime == 0 && update.deviceId().compareTo(deviceId) > 0);
    }

    /** Aplica el cambio solo si gana la comparacion LWW. Devuelve si se aplico. */
    public boolean merge(ProgressUpdate update, int comicPages, Instant now) {
        if (!isOlderThan(update)) {
            return false;
        }
        validatePage(update.currentPage(), comicPages);
        currentPage = update.currentPage();
        totalPages = comicPages;
        currentPanel = update.currentPanel();
        guidedMode = update.guidedMode();
        clientUpdatedAt = truncate(update.clientUpdatedAt());
        deviceId = update.deviceId();
        serverUpdatedAt = truncate(now);
        return true;
    }

    public boolean isFinished() {
        return currentPage == totalPages;
    }

    /** Porcentaje leido, de 0 a 100, con un decimal. */
    public double percent() {
        return Math.round(currentPage * 1000.0 / totalPages) / 10.0;
    }

    private static void validatePage(int page, int total) {
        if (total < 1) {
            throw new IllegalArgumentException("El comic debe tener al menos una pagina");
        }
        if (page < 1 || page > total) {
            throw new InvalidInputException("reading.invalid_page", "La pagina debe estar entre 1 y " + total);
        }
    }

    private static Instant truncate(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    public UserId user() {
        return user;
    }

    public ComicId comic() {
        return comic;
    }

    public int currentPage() {
        return currentPage;
    }

    public int totalPages() {
        return totalPages;
    }

    public int currentPanel() {
        return currentPanel;
    }

    public boolean guidedMode() {
        return guidedMode;
    }

    public Instant clientUpdatedAt() {
        return clientUpdatedAt;
    }

    public String deviceId() {
        return deviceId;
    }

    public Instant serverUpdatedAt() {
        return serverUpdatedAt;
    }
}
