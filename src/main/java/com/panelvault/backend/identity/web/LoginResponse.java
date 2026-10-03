package com.panelvault.backend.identity.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.panelvault.backend.identity.application.AuthTokens;
import com.panelvault.backend.identity.application.IssuedChallenge;
import com.panelvault.backend.identity.application.LoginResult;
import java.time.Instant;

/**
 * Respuesta de {@code POST /api/v1/auth/login}. El campo {@code status} indica el caso:
 *
 * <ul>
 *   <li>{@code AUTHENTICATED}: trae los tokens, igual que {@link TokenResponse}.</li>
 *   <li>{@code TWO_FACTOR_REQUIRED}: trae {@code challengeToken}; el cliente debe pedir el codigo de
 *       la app y enviarlo a {@code /api/v1/auth/2fa/verify}.</li>
 * </ul>
 * Los campos que no aplican se omiten del JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        String status,
        String tokenType,
        String accessToken,
        Long expiresIn,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        String challengeToken,
        Instant challengeExpiresAt) {

    public static final String AUTHENTICATED = "AUTHENTICATED";
    public static final String TWO_FACTOR_REQUIRED = "TWO_FACTOR_REQUIRED";

    public static LoginResponse from(LoginResult result) {
        return switch (result) {
            case LoginResult.Authenticated authenticated -> authenticated(authenticated.tokens());
            case LoginResult.TwoFactorRequired required -> twoFactorRequired(required.challenge());
        };
    }

    private static LoginResponse authenticated(AuthTokens tokens) {
        return new LoginResponse(
                AUTHENTICATED,
                "Bearer",
                tokens.accessToken(),
                tokens.accessTokenExpiresInSeconds(),
                tokens.accessTokenExpiresAt(),
                tokens.refreshToken(),
                tokens.refreshTokenExpiresAt(),
                null,
                null);
    }

    private static LoginResponse twoFactorRequired(IssuedChallenge challenge) {
        return new LoginResponse(
                TWO_FACTOR_REQUIRED, null, null, null, null, null, null, challenge.token(), challenge.expiresAt());
    }

    @Override
    public String toString() {
        return "LoginResponse[status=" + status + ", tokens=***]";
    }
}