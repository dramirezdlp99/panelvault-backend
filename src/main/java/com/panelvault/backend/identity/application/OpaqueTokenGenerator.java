package com.panelvault.backend.identity.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Genera refresh tokens opacos y calcula su huella SHA-256.
 *
 * <p>Un token opaco no contiene datos: son 32 bytes aleatorios (256 bits) de {@link SecureRandom},
 * imposibles de adivinar. Se codifican en Base64 URL-safe sin relleno (43 caracteres) para viajar
 * sin problemas en JSON, cabeceras o cookies.
 *
 * <p>Para guardarlo basta SHA-256 y no hace falta BCrypt: BCrypt es lento para frenar ataques de
 * diccionario contra contrasenas humanas, pero un valor de 256 bits aleatorios no tiene diccionario.
 */
public class OpaqueTokenGenerator {

    static final int TOKEN_BYTES = 32;

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom random;

    public OpaqueTokenGenerator() {
        this(new SecureRandom());
    }

    OpaqueTokenGenerator(SecureRandom random) {
        this.random = Objects.requireNonNull(random);
    }

    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }

    /** Huella SHA-256 en hexadecimal (64 caracteres). Es lo unico que se guarda en la base. */
    public static String sha256Hex(String token) {
        Objects.requireNonNull(token, "El token es obligatorio");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // Toda JVM esta obligada a incluir SHA-256; si falta, el entorno esta roto.
            throw new IllegalStateException("SHA-256 no esta disponible en esta JVM", e);
        }
    }
}