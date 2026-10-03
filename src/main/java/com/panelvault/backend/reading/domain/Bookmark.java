package com.panelvault.backend.reading.domain;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.shared.error.InvalidInputException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Marcador en una pagina de un comic, con una nota opcional (Entidad).
 *
 * <p>Como los comics, su id lo genera el navegador: un marcador creado sin conexion se sincroniza
 * despues sin riesgo de duplicarse.
 */
public final class Bookmark {

    public static final int MAX_NOTE = 200;

    private final UUID id;
    private final UserId user;
    private final ComicId comic;
    private int page;
    private String note;
    private final Instant createdAt;

    private Bookmark(UUID id, UserId user, ComicId comic, int page, String note, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.user = Objects.requireNonNull(user);
        this.comic = Objects.requireNonNull(comic);
        this.createdAt = Objects.requireNonNull(createdAt).truncatedTo(ChronoUnit.MICROS);
        change(page, note);
    }

    public static Bookmark create(UUID id, UserId user, ComicId comic, int page, String note, Instant now) {
        return new Bookmark(id, user, comic, page, note, now);
    }

    public static Bookmark rehydrate(UUID id, UserId user, ComicId comic, int page, String note, Instant createdAt) {
        return new Bookmark(id, user, comic, page, note, createdAt);
    }

    public boolean isOwnedBy(UserId other) {
        return user.equals(other);
    }

    public void change(int newPage, String newNote) {
        if (newPage < 1) {
            throw new InvalidInputException("reading.invalid_page", "La pagina debe ser 1 o mayor");
        }
        String clean = newNote == null ? "" : newNote.strip();
        if (clean.length() > MAX_NOTE) {
            throw new InvalidInputException("reading.invalid_note", "La nota admite maximo " + MAX_NOTE + " caracteres");
        }
        page = newPage;
        note = clean;
    }

    public UUID id() {
        return id;
    }

    public UserId user() {
        return user;
    }

    public ComicId comic() {
        return comic;
    }

    public int page() {
        return page;
    }

    public String note() {
        return note;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
