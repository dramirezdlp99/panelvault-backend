package com.panelvault.backend.identity.web;

import com.panelvault.backend.identity.application.AuthTokens;
import java.time.Instant;

/**
 * Respuesta de login y refresh. Sigue la forma habitual de OAuth 2 ({@code tokenType},
 * {@code expiresIn}) para que cualquier cliente la entienda.
 *
 * <p>Quien la recibe es el servidor de Next.js, que guarda ambos tokens en cookies httpOnly: el
 * JavaScript del navegador nunca los ve, asi un ataque XSS no puede robarlos.
 */
public record TokenResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {

    public static TokenResponse from(AuthTokens tokens) {
        return new TokenResponse(
                "Bearer",
                tokens.accessToken(),
                tokens.accessTokenExpiresInSeconds(),
                tokens.accessTokenExpiresAt(),
                tokens.refreshToken(),
                tokens.refreshTokenExpiresAt());
    }

    @Override
    public String toString() {
        return "TokenResponse[expiresIn=" + expiresIn + ", tokens=***]";
    }
}