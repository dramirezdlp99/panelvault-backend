package com.panelvault.backend.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la tabla {@code users} (modelo de persistencia).
 *
 * <p>Es distinta de la entidad de dominio {@code User} a proposito: JPA exige un constructor vacio
 * y atributos mutables, cosas que el dominio no deberia tener. El adaptador traduce entre ambas.
 *
 * <p>El esquema lo crea Flyway (V1); Hibernate solo lo valida al arrancar ({@code ddl-auto: validate}).
 */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "display_name", nullable = false, length = 40)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Requerido por JPA. */
    protected UserJpaEntity() {}

    public UserJpaEntity(
            UUID id,
            String email,
            String displayName,
            String passwordHash,
            String role,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}