package com.panelvault.backend.identity.infrastructure.security;

import com.panelvault.backend.identity.application.SecretProtector;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Implementa {@link SecretProtector} con AES-256-GCM y HMAC-SHA256.
 *
 * <p><b>Cifrado:</b> AES en modo GCM cifra y ademas autentica: si alguien altera un solo bit del
 * texto cifrado, descifrar falla en vez de devolver basura. Cada cifrado usa un IV (nonce) aleatorio
 * de 12 bytes, que se guarda delante del texto: {@code Base64(IV || cifrado || etiqueta)}. El
 * contexto (id del usuario) entra como "dato adicional autenticado": no se cifra, pero si no
 * coincide al descifrar, falla.
 *
 * <p><b>Huellas:</b> HMAC-SHA256 con una subclave derivada de la clave maestra. Se usa una subclave
 * distinta para no emplear la misma clave en dos algoritmos (separacion de claves).
 */
public class AesGcmSecretProtector implements SecretProtector {

    static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey encryptionKey;
    private final SecretKey fingerprintKey;
    private final SecureRandom random = new SecureRandom();

    public AesGcmSecretProtector(String base64Key) {
        byte[] master = decodeKey(base64Key);
        this.encryptionKey = new SecretKeySpec(master, "AES");
        this.fingerprintKey = new SecretKeySpec(hmac(master, "panelvault-recovery-codes"), "HmacSHA256");
    }

    @Override
    public String encrypt(byte[] plaintext, String context) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plaintext);
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el secreto", e);
        }
    }

    @Override
    public byte[] decrypt(String ciphertext, String context) {
        try {
            byte[] data = Base64.getDecoder().decode(ciphertext);
            if (data.length <= IV_BYTES) {
                throw new IllegalStateException("El texto cifrado esta incompleto");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // AEADBadTagException cae aqui: el texto fue alterado o el contexto no coincide.
            throw new IllegalStateException("No se pudo descifrar el secreto", e);
        }
    }

    @Override
    public String fingerprint(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(fingerprintKey);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular la huella", e);
        }
    }

    private static byte[] decodeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("Falta la clave de cifrado de la 2FA (variable PANELVAULT_TOTP_ENCRYPTION_KEY)");
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64Key.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("PANELVAULT_TOTP_ENCRYPTION_KEY debe estar en Base64", e);
        }
        if (key.length != KEY_BYTES) {
            throw new IllegalStateException(
                    "PANELVAULT_TOTP_ENCRYPTION_KEY debe tener exactamente " + KEY_BYTES + " bytes (AES-256); tiene " + key.length);
        }
        return key;
    }

    /** HMAC-SHA256(clave, etiqueta): deriva una subclave independiente para otro uso. */
    static byte[] hmac(byte[] key, String label) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(label.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 no esta disponible en esta JVM", e);
        }
    }
}