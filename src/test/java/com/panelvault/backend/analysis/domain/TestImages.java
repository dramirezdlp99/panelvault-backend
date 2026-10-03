package com.panelvault.backend.analysis.domain;

import java.util.Base64;

/** Imagenes minimas para pruebas: solo importan sus primeros bytes (la firma del formato). */
public final class TestImages {

    /** PNG real de 1x1 pixel. */
    public static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    /** Empieza como un JPEG (FF D8 FF E0). */
    public static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'};

    /** Empieza como un WebP ("RIFF" + tamano + "WEBP"). */
    public static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    private TestImages() {}

    /** Un PNG distinto en cada llamada (cambia un byte al final), para obtener huellas distintas. */
    public static byte[] pngVariant(int seed) {
        byte[] copy = java.util.Arrays.copyOf(PNG, PNG.length + 4);
        copy[PNG.length] = (byte) seed;
        copy[PNG.length + 1] = (byte) (seed >> 8);
        copy[PNG.length + 2] = (byte) (seed >> 16);
        copy[PNG.length + 3] = (byte) (seed >> 24);
        return copy;
    }
}
