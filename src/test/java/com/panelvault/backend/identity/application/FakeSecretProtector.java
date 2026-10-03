package com.panelvault.backend.identity.application;

import java.util.Base64;

/**
 * Protector falso para pruebas: "cifra" con Base64 y una marca, y respeta el contexto igual que el
 * real (descifrar con otro contexto falla). No ofrece seguridad; solo hace visible el comportamiento.
 */
public class FakeSecretProtector implements SecretProtector {

    @Override
    public String encrypt(byte[] plaintext, String context) {
        return "enc:" + context + ":" + Base64.getEncoder().encodeToString(plaintext);
    }

    @Override
    public byte[] decrypt(String ciphertext, String context) {
        String prefix = "enc:" + context + ":";
        if (!ciphertext.startsWith(prefix)) {
            throw new IllegalStateException("No se pudo descifrar el secreto");
        }
        return Base64.getDecoder().decode(ciphertext.substring(prefix.length()));
    }

    @Override
    public String fingerprint(String value) {
        return "fp:" + value;
    }
}