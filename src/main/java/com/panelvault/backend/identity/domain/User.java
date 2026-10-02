package com.panelvault.backend.identity.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Usuario de PanelVault (Entidad y raiz de agregado del modulo identity).
 *
 * <p>No sabe nada de JPA, HTTP ni BCrypt: guarda el hash ya calculado y aplica sus propias reglas.
 * Su identidad es el {@link UserId}: dos usuarios son iguales si tienen el mismo id.
 *
 * <p>Las fechas se truncan a microsegundos porque es la precision de PostgreSQL; asi el objeto en
 * memoria y el leido de la base coinciden exactamente.
 */
public final class User {

    private final UserId id;
    private final Email email;
    private final DisplayName displayName;
    private final String passwordHash;
    private Role role;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(
            UserId id,
            Email email,
            DisplayName displayName,
            String passwordHash,
            Role role,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "El id es obligatorio");
        this.email = Objects.requireNonNull(email, "El correo es obligatorio");
        this.displayName = Objects.requireNonNull(displayName, "El nombre es obligatorio");
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("El hash de la contrasena es obligatorio");
        }
        this.passwordHash = passwordHash;
        this.role = Objects.requireNonNull(role, "El rol es obligatorio");
        this.createdAt = truncate(Objects.requireNonNull(createdAt, "La fecha de creacion es obligatoria"));
        this.updatedAt = truncate(Objects.requireNonNull(updatedAt, "La fecha de actualizacion es obligatoria"));
    }

    /** Crea una cuenta nueva. Toda cuenta nace como {@link Role#LECTOR}. */
    public static User register(Email email, DisplayName displayName, String passwordHash, Instant now) {
        return new User(UserId.newId(), email, displayName, passwordHash, Role.LECTOR, now, now);
    }

    /** Reconstruye un usuario ya existente (por ejemplo, leido de la base de datos). */
    public static User rehydrate(
            UserId id,
            Email email,
            DisplayName displayName,
            String passwordHash,
            Role role,
            Instant createdAt,
            Instant updatedAt) {
        return new User(id, email, displayName, passwordHash, role, createdAt, updatedAt);
    }

    /** Cambia el rol. Quien puede hacerlo lo decide la capa de aplicacion (solo ADMIN). */
    public void changeRole(Role newRole, Instant now) {
        Objects.requireNonNull(newRole, "El rol es obligatorio");
        if (newRole != role) {
            role = newRole;
            updatedAt = truncate(now);
        }
    }

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public DisplayName displayName() {
        return displayName;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private static Instant truncate(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof User that && id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Nunca incluye el hash de la contrasena: los toString terminan en los logs. */
    @Override
    public String toString() {
        return "User[id=" + id + ", email=" + email + ", role=" + role + "]";
    }
}