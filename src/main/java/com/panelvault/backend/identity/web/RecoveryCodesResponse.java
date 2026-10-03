package com.panelvault.backend.identity.web;

import java.util.List;

/**
 * Codigos de recuperacion recien generados. Es la UNICA vez que se muestran: en la base solo
 * quedan sus huellas, asi que el usuario debe guardarlos en ese momento.
 */
public record RecoveryCodesResponse(List<String> recoveryCodes) {

    @Override
    public String toString() {
        return "RecoveryCodesResponse[codes=***]";
    }
}