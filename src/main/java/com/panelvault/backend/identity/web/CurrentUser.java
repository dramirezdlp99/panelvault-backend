package com.panelvault.backend.identity.web;

import com.panelvault.backend.identity.domain.UserId;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Utilidad para obtener el id del usuario autenticado a partir del claim {@code sub} del JWT. */
final class CurrentUser {

    private CurrentUser() {}

    static UserId idOf(Jwt jwt) {
        return new UserId(UUID.fromString(jwt.getSubject()));
    }
}