package com.panelvault.backend.identity.web;

import com.panelvault.backend.identity.application.TwoFactorEnrollment;

/**
 * Respuesta al iniciar la activacion de la 2FA. El frontend dibuja {@code otpauthUri} como codigo
 * QR y muestra {@code secret} por si el usuario prefiere escribirlo a mano en su app.
 */
public record TwoFactorSetupResponse(String secret, String otpauthUri) {

    public static TwoFactorSetupResponse from(TwoFactorEnrollment enrollment) {
        return new TwoFactorSetupResponse(enrollment.secretBase32(), enrollment.otpAuthUri());
    }

    @Override
    public String toString() {
        return "TwoFactorSetupResponse[secret=***]";
    }
}