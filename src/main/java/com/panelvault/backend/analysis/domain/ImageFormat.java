package com.panelvault.backend.analysis.domain;

import java.util.Optional;

/**
 * Formatos de imagen aceptados, reconocidos por su firma binaria ("numero magico"), no por la
 * extension ni por la cabecera Content-Type, que el cliente puede falsear.
 *
 * <ul>
 *   <li>JPEG: empieza con {@code FF D8 FF}.</li>
 *   <li>PNG: empieza con {@code 89 50 4E 47 0D 0A 1A 0A}.</li>
 *   <li>WebP: {@code "RIFF"}, 4 bytes de tamano y luego {@code "WEBP"}.</li>
 * </ul>
 */
public enum ImageFormat {

    JPEG,
    PNG,
    WEBP;

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_TAG = {'W', 'E', 'B', 'P'};

    public static Optional<ImageFormat> detect(byte[] data) {
        if (data == null) {
            return Optional.empty();
        }
        if (startsWith(data, 0, JPEG_MAGIC)) {
            return Optional.of(JPEG);
        }
        if (startsWith(data, 0, PNG_MAGIC)) {
            return Optional.of(PNG);
        }
        if (startsWith(data, 0, RIFF) && startsWith(data, 8, WEBP_TAG)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        if (data.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
