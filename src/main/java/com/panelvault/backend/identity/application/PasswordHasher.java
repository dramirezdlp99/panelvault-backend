package com.panelvault.backend.identity.application;

/**
 * Puerto para calcular y verificar hashes de contrasenas.
 *
 * <p>La aplicacion no sabe que algoritmo se usa (hoy BCrypt). Si manana se cambia a Argon2, solo
 * cambia el adaptador.
 */
public interface PasswordHasher {

    /** Calcula el hash de una contrasena en texto plano. Cada llamada usa una sal distinta. */
    String hash(String rawPassword);

    /** Indica si la contrasena en texto plano corresponde al hash guardado. */
    boolean matches(String rawPassword, String passwordHash);
}