package com.panelvault.backend.library.domain;

import com.panelvault.backend.identity.domain.UserId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Comic de la biblioteca de un usuario (Entidad y raiz de agregado).
 *
 * <p>El servidor guarda solo los METADATOS: el archivo y sus paginas viven en el navegador del
 * usuario (IndexedDB), que es lo que permite leer sin conexion. Asi la biblioteca se ve igual en
 * todos los dispositivos sin que el servidor almacene la obra.
 *
 * <p>{@code version} sube con cada cambio; el frontend la usa para saber si su copia local esta
 * desactualizada.
 */
public final class Comic {

    private final ComicId id;
    private final UserId owner;
    private ComicDetails details;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    private Comic(ComicId id, UserId owner, ComicDetails details, Instant createdAt, Instant updatedAt, long version) {
        this.id = Objects.requireNonNull(id);
        this.owner = Objects.requireNonNull(owner);
        this.details = Objects.requireNonNull(details);
        this.createdAt = truncate(Objects.requireNonNull(createdAt));
        this.updatedAt = truncate(Objects.requireNonNull(updatedAt));
        if (version < 1) {
            throw new IllegalArgumentException("La version empieza en 1");
        }
        this.version = version;
    }

    public static Comic add(ComicId id, UserId owner, ComicDetails details, Instant now) {
        return new Comic(id, owner, details, now, now, 1);
    }

    public static Comic rehydrate(
            ComicId id, UserId owner, ComicDetails details, Instant createdAt, Instant updatedAt, long version) {
        return new Comic(id, owner, details, createdAt, updatedAt, version);
    }

    public boolean isOwnedBy(UserId user) {
        return owner.equals(user);
    }

    /** Cambia los datos. Si no cambio nada, no sube la version (la operacion es idempotente). */
    public boolean update(ComicDetails newDetails, Instant now) {
        Objects.requireNonNull(newDetails);
        if (newDetails.equals(details)) {
            return false;
        }
        details = newDetails;
        updatedAt = truncate(now);
        version++;
        return true;
    }

    public ComicId id() {
        return id;
    }

    public UserId owner() {
        return owner;
    }

    public ComicDetails details() {
        return details;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }

    private static Instant truncate(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Comic that && id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Comic[id=" + id + ", title=" + details.title() + ", version=" + version + "]";
    }
}
