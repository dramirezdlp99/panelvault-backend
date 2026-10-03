package com.panelvault.backend.identity.application;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Genera y normaliza codigos de recuperacion de la 2FA.
 *
 * <p>Cada codigo tiene 10 caracteres de un alfabeto de 32 simbolos sin caracteres confusos (no hay
 * 0, O, 1 ni I): 32^10 = 2^50 combinaciones. Se muestran como {@code ABCDE-FGHJK} para que sea
 * facil copiarlos a mano, y al recibirlos se aceptan con o sin guion y en minusculas.
 */
public class RecoveryCodes {

    public static final int COUNT = 10;
    static final int LENGTH = 10;
    static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final SecureRandom random;

    public RecoveryCodes() {
        this(new SecureRandom());
    }

    RecoveryCodes(SecureRandom random) {
        this.random = Objects.requireNonNull(random);
    }

    /** Genera {@link #COUNT} codigos distintos, ya formateados para mostrar. */
    public List<String> generate() {
        List<String> codes = new ArrayList<>(COUNT);
        while (codes.size() < COUNT) {
            StringBuilder code = new StringBuilder(LENGTH + 1);
            for (int i = 0; i < LENGTH; i++) {
                if (i == LENGTH / 2) {
                    code.append('-');
                }
                code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            String formatted = code.toString();
            if (!codes.contains(formatted)) {
                codes.add(formatted);
            }
        }
        return List.copyOf(codes);
    }

    /** Quita espacios y guiones y pasa a mayusculas. Es la forma que se guarda (como huella). */
    public static String normalize(String code) {
        return code == null ? "" : code.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    /** Indica si un texto tiene forma de codigo de recuperacion (y no de codigo TOTP). */
    public static boolean looksLikeRecoveryCode(String code) {
        String normalized = normalize(code);
        if (normalized.length() != LENGTH) {
            return false;
        }
        for (int i = 0; i < normalized.length(); i++) {
            if (ALPHABET.indexOf(normalized.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }
}