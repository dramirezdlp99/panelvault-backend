package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Emisor falso de tickets del segundo paso: el ticket es legible ({@code challenge:<id>}). */
public class FakeLoginChallengeIssuer implements LoginChallengeIssuer {

    private static final String PREFIX = "challenge:";

    @Override
    public IssuedChallenge issue(UserId userId, Instant now) {
        return new IssuedChallenge(PREFIX + userId, now.plus(Duration.ofMinutes(5)));
    }

    @Override
    public UserId verify(String token) {
        try {
            if (token == null || !token.startsWith(PREFIX)) {
                throw new IllegalArgumentException();
            }
            return new UserId(UUID.fromString(token.substring(PREFIX.length())));
        } catch (IllegalArgumentException e) {
            throw new UnauthenticatedException("auth.challenge_invalid", "La verificacion expiro. Inicia sesion de nuevo");
        }
    }
}