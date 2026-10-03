package com.panelvault.backend.identity.application;

import java.time.Instant;

/** Ticket temporal que demuestra que la contrasena ya fue verificada y falta el segundo paso. */
public record IssuedChallenge(String token, Instant expiresAt) {

    @Override
    public String toString() {
        return "IssuedChallenge[expiresAt=" + expiresAt + ", token=***]";
    }
}