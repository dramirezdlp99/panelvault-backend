package com.panelvault.backend.identity.web;

import com.panelvault.backend.identity.domain.User;
import java.time.Instant;
import java.util.UUID;

/**
 * Representacion publica de un usuario. Nunca incluye el hash de la contrasena.
 */
public record UserResponse(UUID id, String email, String displayName, String role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.id().value(),
                user.email().value(),
                user.displayName().value(),
                user.role().name(),
                user.createdAt());
    }
}