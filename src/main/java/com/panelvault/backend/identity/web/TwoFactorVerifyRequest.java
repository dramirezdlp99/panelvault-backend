package com.panelvault.backend.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo JSON de {@code POST /api/v1/auth/2fa/verify}: el ticket del primer paso y el codigo de 6
 * digitos de la app (o un codigo de recuperacion).
 */
public record TwoFactorVerifyRequest(
        @NotBlank @Size(max = 2000) String challengeToken, @NotBlank @Size(max = 20) String code) {

    @Override
    public String toString() {
        return "TwoFactorVerifyRequest[challengeToken=***, code=***]";
    }
}