package com.panelvault.backend.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la tabla {@code user_two_factor} (creada por Flyway en V3).
 *
 * <p>Las huellas de los codigos de recuperacion se guardan juntas, separadas por comas, en una sola
 * columna: pertenecen al agregado y siempre se leen y escriben enteras.
 */
@Entity
@Table(name = "user_two_factor")
public class TwoFactorJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "secret_ciphertext", nullable = false, length = 200)
    private String secretCiphertext;

    @Column(name = "recovery_codes", nullable = false, length = 700)
    private String recoveryCodes;

    @Column(name = "last_used_step")
    private Long lastUsedStep;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "enabled_at")
    private Instant enabledAt;

    /** Requerido por JPA. */
    protected TwoFactorJpaEntity() {}

    public TwoFactorJpaEntity(
            UUID userId,
            String secretCiphertext,
            String recoveryCodes,
            Long lastUsedStep,
            Instant createdAt,
            Instant enabledAt) {
        this.userId = userId;
        this.secretCiphertext = secretCiphertext;
        this.recoveryCodes = recoveryCodes;
        this.lastUsedStep = lastUsedStep;
        this.createdAt = createdAt;
        this.enabledAt = enabledAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSecretCiphertext() {
        return secretCiphertext;
    }

    public String getRecoveryCodes() {
        return recoveryCodes;
    }

    public Long getLastUsedStep() {
        return lastUsedStep;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEnabledAt() {
        return enabledAt;
    }
}