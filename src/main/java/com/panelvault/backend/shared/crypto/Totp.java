package com.panelvault.backend.shared.crypto;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.OptionalLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Contrasenas de un solo uso basadas en tiempo: TOTP (RFC 6238) sobre HOTP (RFC 4226).
 *
 * <p>El servidor y la app del usuario comparten un secreto. Cada 30 segundos ambos calculan, cada
 * uno por su lado, el mismo codigo de 6 digitos:
 * <ol>
 *   <li>{@code T = floor(segundosUnix / 30)}: numero del intervalo de tiempo actual.</li>
 *   <li>{@code H = HMAC-SHA1(secreto, T como 8 bytes big-endian)}: 20 bytes.</li>
 *   <li>Truncado dinamico: los 4 bits bajos del ultimo byte dan un desplazamiento {@code o};
 *       se leen 4 bytes desde {@code H[o]} y se descarta el bit de signo (31 bits).</li>
 *   <li>Codigo = ese numero modulo 10^6, rellenado con ceros a la izquierda.</li>
 * </ol>
 * Como el codigo depende de la hora, un codigo visto hoy no sirve manana. La verificacion acepta
 * el intervalo anterior y el siguiente para tolerar relojes un poco desfasados.
 */
public final class Totp {

    public static final int DIGITS = 6;
    public static final int PERIOD_SECONDS = 30;
    /** 160 bits: el tamano que recomienda el RFC 4226 para HMAC-SHA1. */
    public static final int SECRET_BYTES = 20;

    private static final int[] POWERS_OF_TEN = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000};

    private Totp() {}

    public static byte[] newSecret(SecureRandom random) {
        byte[] secret = new byte[SECRET_BYTES];
        random.nextBytes(secret);
        return secret;
    }

    /** Numero del intervalo de 30 s al que pertenece un instante. */
    public static long timeStep(Instant instant) {
        return Math.floorDiv(instant.getEpochSecond(), PERIOD_SECONDS);
    }

    /** Codigo de 6 digitos para un intervalo dado. */
    public static String code(byte[] secret, long timeStep) {
        return hotp(secret, timeStep, DIGITS);
    }

    /** HOTP (RFC 4226) con el numero de digitos indicado (6 a 8). */
    public static String hotp(byte[] secret, long counter, int digits) {
        if (digits < 6 || digits > 8) {
            throw new IllegalArgumentException("El codigo debe tener entre 6 y 8 digitos");
        }
        byte[] hash = hmacSha1(secret, ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int otp = binary % POWERS_OF_TEN[digits];
        return String.format("%0" + digits + "d", otp);
    }

    /**
     * Verifica un codigo aceptando {@code window} intervalos antes y despues del actual.
     *
     * <p>Devuelve el intervalo que coincidio (para impedir reusar el mismo codigo) o vacio. Se
     * revisan todos los intervalos y se compara en tiempo constante, para que el tiempo de
     * respuesta no revele cuantos digitos acertaron.
     */
    public static OptionalLong verify(byte[] secret, String code, Instant now, int window) {
        if (code == null || code.length() != DIGITS || !code.chars().allMatch(Character::isDigit)) {
            return OptionalLong.empty();
        }
        long current = timeStep(now);
        byte[] given = code.getBytes(StandardCharsets.US_ASCII);
        OptionalLong match = OptionalLong.empty();
        for (long step = current - window; step <= current + window; step++) {
            byte[] expected = code(secret, step).getBytes(StandardCharsets.US_ASCII);
            if (MessageDigest.isEqual(expected, given) && match.isEmpty()) {
                match = OptionalLong.of(step);
            }
        }
        return match;
    }

    /**
     * URI {@code otpauth://} que entienden las apps autenticadoras; el frontend la muestra como
     * codigo QR. Formato documentado por Google Authenticator ("Key Uri Format").
     */
    public static String otpAuthUri(String issuer, String accountName, byte[] secret) {
        String label = encode(issuer) + ":" + encode(accountName);
        return "otpauth://totp/" + label
                + "?secret=" + Base32.encode(secret)
                + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + PERIOD_SECONDS;
    }

    private static String encode(String value) {
        // URLEncoder es para formularios (espacio = '+'); en una URI el espacio va como %20.
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static byte[] hmacSha1(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(message);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 no esta disponible en esta JVM", e);
        }
    }
}