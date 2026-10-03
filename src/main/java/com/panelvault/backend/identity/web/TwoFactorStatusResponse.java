package com.panelvault.backend.identity.web;

import com.panelvault.backend.identity.application.TwoFactorStatus;

/** Estado de la 2FA del usuario autenticado. */
public record TwoFactorStatusResponse(boolean enabled, int recoveryCodesRemaining) {

    public static TwoFactorStatusResponse from(TwoFactorStatus status) {
        return new TwoFactorStatusResponse(status.enabled(), status.recoveryCodesRemaining());
    }
}