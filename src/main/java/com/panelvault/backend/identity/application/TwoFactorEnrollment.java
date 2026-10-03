package com.panelvault.backend.identity.application;

/**
 * Datos para que el usuario agregue PanelVault a su app autenticadora: la URI (que el frontend
 * muestra como codigo QR) y el secreto en Base32 por si prefiere escribirlo a mano.
 */
public record TwoFactorEnrollment(String secretBase32, String otpAuthUri) {

    @Override
    public String toString() {
        return "TwoFactorEnrollment[secret=***]";
    }
}