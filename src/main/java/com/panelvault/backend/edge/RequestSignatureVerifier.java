package com.panelvault.backend.edge;

import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifica la firma HMAC-SHA256 con que el servidor de Next.js (el gateway) firma cada peticion.
 *
 * <p>Texto canonico: {@code "{timestamp}\n{METODO}\n{ruta con query}\n{sha256hex(cuerpo)}"}. A
 * diferencia de la firma hacia el motor de IA, aqui la query SI se firma: en esta API cambia el
 * significado de la peticion (por ejemplo {@code ?preset=manga}).
 *
 * <p>Con la firma activa, aunque alguien descubra la URL del backend en Render, no puede llamarlo
 * directamente: solo el frontend conoce el secreto.
 */
public class RequestSignatureVerifier {

    public static final String TIMESTAMP_HEADER = "X-PanelVault-Timestamp";
    public static final String SIGNATURE_HEADER = "X-PanelVault-Signature";
    public static final int MIN_SECRET_LENGTH = 32;

    private final byte[] secret;
    private final Duration maxClockSkew;

    public RequestSignatureVerifier(String secret, Duration maxClockSkew) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "PANELVAULT_GATEWAY_SECRET debe tener al menos " + MIN_SECRET_LENGTH + " caracteres");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.maxClockSkew = maxClockSkew;
    }

    public static String canonicalMessage(String timestamp, String method, String pathAndQuery, byte[] body) {
        return timestamp + "\n" + method.toUpperCase(Locale.ROOT) + "\n" + pathAndQuery + "\n" + sha256Hex(body);
    }

    public String sign(String timestamp, String method, String pathAndQuery, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(
                    canonicalMessage(timestamp, method, pathAndQuery, body).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 no esta disponible en esta JVM", e);
        }
    }

    /**
     * @throws UnauthenticatedException si falta la firma, expiro o no coincide
     */
    public void verify(
            String timestamp, String signature, String method, String pathAndQuery, byte[] body, Instant now) {
        if (timestamp == null || signature == null || timestamp.isBlank() || signature.isBlank()) {
            throw new UnauthenticatedException("gateway.missing_signature", "La peticion no esta firmada");
        }
        long sentAt;
        try {
            sentAt = Long.parseLong(timestamp.strip());
        } catch (NumberFormatException e) {
            throw new UnauthenticatedException("gateway.invalid_signature", "La firma de la peticion no es valida");
        }
        if (Math.abs(now.getEpochSecond() - sentAt) > maxClockSkew.toSeconds()) {
            throw new UnauthenticatedException("gateway.expired_signature", "La firma de la peticion expiro");
        }
        byte[] expected = sign(timestamp.strip(), method, pathAndQuery, body).getBytes(StandardCharsets.US_ASCII);
        byte[] given = signature.strip().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        // Comparacion en tiempo constante: no revela cuantos caracteres coinciden.
        if (!MessageDigest.isEqual(expected, given)) {
            throw new UnauthenticatedException("gateway.invalid_signature", "La firma de la peticion no es valida");
        }
    }

    static String sha256Hex(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 no esta disponible en esta JVM", e);
        }
    }
}
