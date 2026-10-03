package com.panelvault.backend.shared.crypto;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

/**
 * Codificacion Base32 segun RFC 4648, seccion 6.
 *
 * <p>Es el formato en que las apps autenticadoras (Google Authenticator, Microsoft Authenticator)
 * reciben el secreto TOTP. Usa solo A-Z y 2-7, asi que se puede dictar o escribir a mano sin
 * confundir 0 con O ni 1 con l.
 *
 * <p>Algoritmo: se toman los bytes como una cadena de bits y se parte en grupos de 5 bits; cada
 * grupo (0 a 31) es un indice en el alfabeto. Al decodificar se hace lo inverso.
 */
public final class Base32 {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int[] LOOKUP = new int[128];

    static {
        java.util.Arrays.fill(LOOKUP, -1);
        for (int i = 0; i < ALPHABET.length; i++) {
            LOOKUP[ALPHABET[i]] = i;
        }
    }

    private Base32() {}

    /** Codifica sin relleno '=' (las apps autenticadoras no lo necesitan). */
    public static String encode(byte[] data) {
        StringBuilder out = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsInBuffer = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsInBuffer += 8;
            while (bitsInBuffer >= 5) {
                out.append(ALPHABET[(buffer >> (bitsInBuffer - 5)) & 0x1F]);
                bitsInBuffer -= 5;
            }
        }
        if (bitsInBuffer > 0) {
            // Los bits sobrantes se completan con ceros por la derecha.
            out.append(ALPHABET[(buffer << (5 - bitsInBuffer)) & 0x1F]);
        }
        return out.toString();
    }

    /**
     * Decodifica tolerando minusculas, espacios, guiones y relleno '=' (asi lo escribe la gente).
     *
     * @throws IllegalArgumentException si contiene un caracter fuera del alfabeto
     */
    public static byte[] decode(String text) {
        String clean = text.replaceAll("[\\s=-]", "").toUpperCase(Locale.ROOT);
        ByteArrayOutputStream out = new ByteArrayOutputStream(clean.length() * 5 / 8);
        int buffer = 0;
        int bitsInBuffer = 0;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            int value = c < 128 ? LOOKUP[c] : -1;
            if (value < 0) {
                throw new IllegalArgumentException("Caracter no valido en Base32: '" + c + "'");
            }
            buffer = (buffer << 5) | value;
            bitsInBuffer += 5;
            if (bitsInBuffer >= 8) {
                out.write((buffer >> (bitsInBuffer - 8)) & 0xFF);
                bitsInBuffer -= 8;
            }
        }
        return out.toByteArray();
    }
}