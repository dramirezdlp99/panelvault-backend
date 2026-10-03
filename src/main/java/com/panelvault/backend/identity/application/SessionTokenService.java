package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.RefreshToken;
import com.panelvault.backend.identity.domain.RefreshTokenRepository;
import com.panelvault.backend.identity.domain.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Emite los pares de tokens de una sesion. Lo usan el login (abre una familia nueva) y el refresh
 * (rota dentro de la misma familia), asi la regla vive en un solo lugar.
 *
 * <p>No lleva anotaciones de Spring: se construye en la configuracion de infraestructura, que le
 * pasa la duracion del refresh token leida de las propiedades.
 */
public class SessionTokenService {

    private final RefreshTokenRepository refreshTokens;
    private final AccessTokenIssuer accessTokens;
    private final OpaqueTokenGenerator generator;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public SessionTokenService(
            RefreshTokenRepository refreshTokens,
            AccessTokenIssuer accessTokens,
            OpaqueTokenGenerator generator,
            Clock clock,
            Duration refreshTokenTtl) {
        this.refreshTokens = Objects.requireNonNull(refreshTokens);
        this.accessTokens = Objects.requireNonNull(accessTokens);
        this.generator = Objects.requireNonNull(generator);
        this.clock = Objects.requireNonNull(clock);
        if (refreshTokenTtl == null || refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
            throw new IllegalArgumentException("La duracion del refresh token debe ser positiva");
        }
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /** Login: abre una familia nueva de refresh tokens. */
    public AuthTokens openSession(User user) {
        Instant now = clock.instant();
        String raw = generator.generate();
        RefreshToken token = RefreshToken.issue(
                user.id(), UUID.randomUUID(), OpaqueTokenGenerator.sha256Hex(raw), now, refreshTokenTtl);
        refreshTokens.save(token);
        return tokens(user, now, raw, token);
    }

    /** Refresh: revoca el token actual y emite su sucesor en la misma familia. */
    public AuthTokens rotate(User user, RefreshToken current) {
        Instant now = clock.instant();
        String raw = generator.generate();
        RefreshToken next = RefreshToken.issue(
                user.id(), current.familyId(), OpaqueTokenGenerator.sha256Hex(raw), now, refreshTokenTtl);
        current.rotateTo(next, now);
        refreshTokens.save(next);
        refreshTokens.save(current);
        return tokens(user, now, raw, next);
    }

    private AuthTokens tokens(User user, Instant now, String rawRefreshToken, RefreshToken refreshToken) {
        IssuedAccessToken access = accessTokens.issue(user, now);
        long expiresIn = Math.max(0, Duration.between(now, access.expiresAt()).toSeconds());
        return new AuthTokens(
                access.value(), access.expiresAt(), expiresIn, rawRefreshToken, refreshToken.expiresAt());
    }
}