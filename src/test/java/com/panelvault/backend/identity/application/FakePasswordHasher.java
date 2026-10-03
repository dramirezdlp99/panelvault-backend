package com.panelvault.backend.identity.application;

/**
 * Hasher falso para pruebas: antepone "hashed:" en vez de correr BCrypt (que tarda ~250 ms).
 * Lleva la cuenta de cuantas veces se uso cada metodo, para verificar por ejemplo que no se hashea
 * en vano o que el login compara siempre una contrasena (aunque el correo no exista).
 */
public class FakePasswordHasher implements PasswordHasher {

    private int hashCalls;
    private int matchesCalls;

    @Override
    public String hash(String rawPassword) {
        hashCalls++;
        return "hashed:" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        matchesCalls++;
        return passwordHash.equals("hashed:" + rawPassword);
    }

    public int hashCalls() {
        return hashCalls;
    }

    public int matchesCalls() {
        return matchesCalls;
    }
}