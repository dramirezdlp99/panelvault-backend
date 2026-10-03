package com.panelvault.backend.analysis.infrastructure.engine;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Firma las peticiones al motor de IA con HMAC-SHA256, exactamente como lo verifica el motor
 * ({@code panelvault_ai/api/security.py}).
 *
 * <p>Texto canonico: {@code "{timestamp}\n{METODO}\n{ruta}\n{sha256hex(cuerpo)}"}. La firma va en
 * hexadecimal en la cabecera {@code X-PanelVault-Signature}, y el timestamp (segundos Unix) en
 * {@code X-PanelVault-Timestamp}. Como el timestamp esta firmado, una peticion capturada deja de
 * servir a los 5 minutos.
 */
public final class HmacRequestSigner {

    public static final String TIMESTAMP_HEADER = "X-PanelVault-Timestamp";
    public static final String SIGNATURE_HEADER = "X-PanelVault-Signature";
    /** El motor exige al menos esta longitud; se valida aqui para fallar al arrancar y no en cada peticion. */
    public static final int MIN_SECRET_LENGTH = 32;

    private final byte[] secret;

    public HmacRequestSigner(String secret) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "PANELVAULT_ENGINE_SECRET debe tener al menos " + MIN_SECRET_LENGTH + " caracteres");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public static String canonicalMessage(long timestamp, String method, String path, byte[] body) {
        return timestamp + "\n" + method.toUpperCase(Locale.ROOT) + "\n" + path + "\n" + sha256Hex(body);
    }

    public String sign(long timestamp, String method, String path, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] signature = mac.doFinal(
                    canonicalMessage(timestamp, method, path, body).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signature);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 no esta disponible en esta JVM", e);
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
