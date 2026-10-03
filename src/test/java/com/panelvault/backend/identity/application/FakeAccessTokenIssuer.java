package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.User;
import java.time.Duration;
import java.time.Instant;

/**
 * Emisor falso de access tokens: el "token" es legible ({@code access:<id>:<rol>:<n>}) para que
 * las pruebas puedan comprobar a quien y con que rol se emitio, sin firmar nada.
 */
public class FakeAccessTokenIssuer implements AccessTokenIssuer {

    public static final Duration TTL = Duration.ofMinutes(15);

    private int issued;

    @Override
    public IssuedAccessToken issue(User user, Instant now) {
        issued++;
        return new IssuedAccessToken("access:" + user.id() + ":" + user.role() + ":" + issued, now.plus(TTL));
    }
}