package com.panelvault.backend.identity.application;

/**
 * Nombre con el que PanelVault aparece en la app autenticadora del usuario (por ejemplo,
 * "PanelVault"). Es un tipo propio, y no un String suelto, para que Spring lo inyecte sin ambiguedad.
 */
public record TwoFactorIssuerLabel(String value) {

    public TwoFactorIssuerLabel {
        if (value == null || value.isBlank() || value.contains(":")) {
            throw new IllegalArgumentException("El nombre del emisor es obligatorio y no puede contener ':'");
        }
    }
}