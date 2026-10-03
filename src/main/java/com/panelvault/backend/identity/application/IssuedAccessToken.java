package com.panelvault.backend.identity.application;

import java.time.Instant;

/** Access token ya firmado junto con su vencimiento. */
public record IssuedAccessToken(String value, Instant expiresAt) {

    @Override
    public String toString() {
        return "IssuedAccessToken[expiresAt=" + expiresAt + ", value=***]";
    }
}