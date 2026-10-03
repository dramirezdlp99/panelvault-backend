package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.RefreshToken;
import com.panelvault.backend.identity.domain.RefreshTokenRepository;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserRepository;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso: refrescar la sesion (rotacion con deteccion de reuso) y cerrar sesion.
 *
 * <p>Detalle importante: al detectar un reuso se revoca la familia y LUEGO se lanza la excepcion.
 * Por defecto Spring deshace la transaccion ante cualquier excepcion, lo que borraria la
 * revocacion justo cuando mas importa. Por eso {@code noRollbackFor}.
 */
@Service
public class TokenRefreshService {

    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final SessionTokenService sessions;
    private final Clock clock;

    public TokenRefreshService(
            RefreshTokenRepository refreshTokens, UserRepository users, SessionTokenService sessions, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = UnauthenticatedException.class)
    public AuthTokens refresh(String rawRefreshToken) {
        RefreshToken current = find(rawRefreshToken);
        Instant now = clock.instant();

        if (current.isRevoked()) {
            refreshTokens.revokeFamily(current.familyId(), now);
            throw new UnauthenticatedException(
                    "auth.refresh_token_reused", "Por seguridad se cerro la sesion. Inicia sesion de nuevo");
        }
        if (current.isExpired(now)) {
            throw new UnauthenticatedException(
                    "auth.refresh_token_expired", "La sesion expiro. Inicia sesion de nuevo");
        }
        User user = users.findById(current.userId()).orElseThrow(TokenRefreshService::invalidToken);
        return sessions.rotate(user, current);
    }

    /** Cierra la sesion revocando su familia. Es idempotente: un token desconocido no es error. */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHashForUpdate(OpaqueTokenGenerator.sha256Hex(rawRefreshToken))
                .ifPresent(token -> refreshTokens.revokeFamily(token.familyId(), clock.instant()));
    }

    private RefreshToken find(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw invalidToken();
        }
        return refreshTokens.findByTokenHashForUpdate(OpaqueTokenGenerator.sha256Hex(rawRefreshToken))
                .orElseThrow(TokenRefreshService::invalidToken);
    }

    private static UnauthenticatedException invalidToken() {
        return new UnauthenticatedException("auth.refresh_token_invalid", "La sesion no es valida. Inicia sesion de nuevo");
    }
}