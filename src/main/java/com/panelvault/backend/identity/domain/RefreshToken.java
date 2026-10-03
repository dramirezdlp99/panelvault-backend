package com.panelvault.backend.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Refresh token de una sesion (Entidad).
 *
 * <p>Ideas clave del diseno:
 * <ul>
 *   <li><b>Solo se guarda el hash</b> (SHA-256) del token, nunca el token. Si alguien roba una copia
 *       de la base de datos, no puede usar esos valores para iniciar sesion.</li>
 *   <li><b>Rotacion:</b> cada token se usa una sola vez. Al refrescar se revoca y se emite uno nuevo
 *       que apunta al anterior ({@code replacedBy}).</li>
 *   <li><b>Familia:</b> todos los tokens que nacen de un mismo login comparten {@code familyId}. Si
 *       un token ya usado vuelve a aparecer, alguien lo copio: se revoca la familia entera y tanto
 *       el ladron como el usuario legitimo deben iniciar sesion otra vez.</li>
 * </ul>
 */
public final class RefreshToken {

    private final UUID id;
    private final UserId userId;
    private final UUID familyId;
    private final String tokenHash;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private Instant revokedAt;
    private UUID replacedBy;

    private RefreshToken(
            UUID id,
            UserId userId,
            UUID familyId,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            Instant revokedAt,
            UUID replacedBy) {
        this.id = Objects.requireNonNull(id, "El id es obligatorio");
        this.userId = Objects.requireNonNull(userId, "El usuario es obligatorio");
        this.familyId = Objects.requireNonNull(familyId, "La familia es obligatoria");
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("El hash del token es obligatorio");
        }
        this.tokenHash = tokenHash;
        this.issuedAt = truncate(Objects.requireNonNull(issuedAt, "La fecha de emision es obligatoria"));
        this.expiresAt = truncate(Objects.requireNonNull(expiresAt, "La fecha de vencimiento es obligatoria"));
        if (!this.expiresAt.isAfter(this.issuedAt)) {
            throw new IllegalArgumentException("El token debe vencer despues de emitirse");
        }
        this.revokedAt = revokedAt == null ? null : truncate(revokedAt);
        this.replacedBy = replacedBy;
    }

    /** Emite un token nuevo dentro de una familia (un login abre una familia nueva). */
    public static RefreshToken issue(UserId userId, UUID familyId, String tokenHash, Instant now, Duration ttl) {
        Objects.requireNonNull(ttl, "La duracion es obligatoria");
        return new RefreshToken(UUID.randomUUID(), userId, familyId, tokenHash, now, now.plus(ttl), null, null);
    }

    /** Reconstruye un token ya guardado. */
    public static RefreshToken rehydrate(
            UUID id,
            UserId userId,
            UUID familyId,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            Instant revokedAt,
            UUID replacedBy) {
        return new RefreshToken(id, userId, familyId, tokenHash, issuedAt, expiresAt, revokedAt, replacedBy);
    }

    /** Vencido desde el instante exacto de {@code expiresAt} en adelante. */
    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    /** Marca este token como usado y enlaza al que lo reemplaza. */
    public void rotateTo(RefreshToken successor, Instant now) {
        Objects.requireNonNull(successor, "El token sucesor es obligatorio");
        if (!successor.familyId.equals(familyId)) {
            throw new IllegalArgumentException("El sucesor debe pertenecer a la misma familia");
        }
        if (isRevoked()) {
            throw new IllegalStateException("Un token revocado no puede rotarse");
        }
        revokedAt = truncate(now);
        replacedBy = successor.id;
    }

    public UUID id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public UUID familyId() {
        return familyId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public UUID replacedBy() {
        return replacedBy;
    }

    private static Instant truncate(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof RefreshToken that && id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "RefreshToken[id=" + id + ", userId=" + userId + ", familyId=" + familyId
                + ", revoked=" + isRevoked() + "]";
    }
}