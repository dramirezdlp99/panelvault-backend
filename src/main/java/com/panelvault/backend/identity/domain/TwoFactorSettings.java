package com.panelvault.backend.identity.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Configuracion de verificacion en dos pasos (2FA) de un usuario (Entidad y raiz de agregado).
 *
 * <p>Ciclo de vida:
 * <ol>
 *   <li><b>Pendiente:</b> se genero el secreto, pero el usuario aun no demostro que lo guardo en su
 *       app. Todavia no se pide en el login.</li>
 *   <li><b>Activa:</b> el usuario confirmo con un primer codigo correcto; se le entregaron sus
 *       codigos de recuperacion.</li>
 * </ol>
 *
 * <p>Los codigos de recuperacion son parte del agregado: se guardan solo sus huellas y cada uno
 * sirve una vez. Ademas se recuerda el ultimo intervalo TOTP usado para que un codigo ya aceptado
 * no pueda usarse de nuevo (proteccion contra repeticion).
 */
public final class TwoFactorSettings {

    private final UserId userId;
    private final String encryptedSecret;
    private final Instant createdAt;
    private Instant enabledAt;
    private Long lastUsedTimeStep;
    private final List<String> recoveryCodeFingerprints;

    private TwoFactorSettings(
            UserId userId,
            String encryptedSecret,
            Instant createdAt,
            Instant enabledAt,
            Long lastUsedTimeStep,
            List<String> recoveryCodeFingerprints) {
        this.userId = Objects.requireNonNull(userId, "El usuario es obligatorio");
        if (encryptedSecret == null || encryptedSecret.isBlank()) {
            throw new IllegalArgumentException("El secreto cifrado es obligatorio");
        }
        this.encryptedSecret = encryptedSecret;
        this.createdAt = truncate(Objects.requireNonNull(createdAt, "La fecha de creacion es obligatoria"));
        this.enabledAt = enabledAt == null ? null : truncate(enabledAt);
        this.lastUsedTimeStep = lastUsedTimeStep;
        this.recoveryCodeFingerprints = new ArrayList<>(Objects.requireNonNull(recoveryCodeFingerprints));
    }

    /** Inicia la activacion con un secreto nuevo (ya cifrado). */
    public static TwoFactorSettings pending(UserId userId, String encryptedSecret, Instant now) {
        return new TwoFactorSettings(userId, encryptedSecret, now, null, null, List.of());
    }

    public static TwoFactorSettings rehydrate(
            UserId userId,
            String encryptedSecret,
            Instant createdAt,
            Instant enabledAt,
            Long lastUsedTimeStep,
            List<String> recoveryCodeFingerprints) {
        return new TwoFactorSettings(
                userId, encryptedSecret, createdAt, enabledAt, lastUsedTimeStep, recoveryCodeFingerprints);
    }

    public boolean isEnabled() {
        return enabledAt != null;
    }

    /** Activa la 2FA guardando las huellas de los codigos de recuperacion recien generados. */
    public void enable(List<String> fingerprints, long confirmedTimeStep, Instant now) {
        if (isEnabled()) {
            throw new IllegalStateException("La verificacion en dos pasos ya esta activa");
        }
        if (fingerprints == null || fingerprints.isEmpty()) {
            throw new IllegalArgumentException("Se necesitan codigos de recuperacion");
        }
        recoveryCodeFingerprints.clear();
        recoveryCodeFingerprints.addAll(fingerprints);
        lastUsedTimeStep = confirmedTimeStep;
        enabledAt = truncate(now);
    }

    /**
     * Acepta un intervalo TOTP solo si es posterior al ultimo usado. Asi, un codigo que alguien vio
     * por encima del hombro no sirve una segunda vez, ni siquiera dentro de sus 30 segundos.
     */
    public boolean acceptTimeStep(long timeStep) {
        if (lastUsedTimeStep != null && timeStep <= lastUsedTimeStep) {
            return false;
        }
        lastUsedTimeStep = timeStep;
        return true;
    }

    /** Consume un codigo de recuperacion si existe. Cada codigo sirve una sola vez. */
    public boolean useRecoveryCode(String fingerprint) {
        if (fingerprint == null) {
            return false;
        }
        byte[] given = fingerprint.getBytes(StandardCharsets.US_ASCII);
        Iterator<String> it = recoveryCodeFingerprints.iterator();
        while (it.hasNext()) {
            // Comparacion en tiempo constante, por buena practica con valores secretos.
            if (MessageDigest.isEqual(it.next().getBytes(StandardCharsets.US_ASCII), given)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public int remainingRecoveryCodes() {
        return recoveryCodeFingerprints.size();
    }

    public UserId userId() {
        return userId;
    }

    public String encryptedSecret() {
        return encryptedSecret;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant enabledAt() {
        return enabledAt;
    }

    public Long lastUsedTimeStep() {
        return lastUsedTimeStep;
    }

    public List<String> recoveryCodeFingerprints() {
        return List.copyOf(recoveryCodeFingerprints);
    }

    private static Instant truncate(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public String toString() {
        return "TwoFactorSettings[userId=" + userId + ", enabled=" + isEnabled()
                + ", recoveryCodes=" + recoveryCodeFingerprints.size() + "]";
    }
}