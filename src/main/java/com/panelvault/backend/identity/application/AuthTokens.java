package com.panelvault.backend.identity.application;

import java.time.Instant;

/**
 * Par de tokens que recibe el cliente al iniciar sesion o refrescar.
 *
 * <ul>
 *   <li>{@code accessToken}: JWT de vida corta (15 min). Va en cada peticion como
 *       {@code Authorization: Bearer ...}. Si lo roban, sirve poco tiempo.</li>
 *   <li>{@code refreshToken}: opaco y de vida larga (7 dias). Solo sirve para pedir un par nuevo y
 *       se usa una unica vez.</li>
 * </ul>
 */
public record AuthTokens(
        String accessToken,
        Instant accessTokenExpiresAt,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        Instant refreshTokenExpiresAt) {

    @Override
    public String toString() {
        return "AuthTokens[accessTokenExpiresAt=" + accessTokenExpiresAt
                + ", refreshTokenExpiresAt=" + refreshTokenExpiresAt + ", tokens=***]";
    }
}