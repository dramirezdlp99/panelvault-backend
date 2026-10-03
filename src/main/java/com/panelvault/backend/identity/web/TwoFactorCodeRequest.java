package com.panelvault.backend.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo JSON con un codigo de verificacion (confirmar o desactivar la 2FA). */
public record TwoFactorCodeRequest(@NotBlank @Size(max = 20) String code) {

    @Override
    public String toString() {
        return "TwoFactorCodeRequest[code=***]";
    }
}