package com.panelvault.backend.identity.application;

/**
 * Hasher falso para pruebas: antepone "hashed:" en vez de correr BCrypt (que tarda ~250 ms).
 * Lleva la cuenta de cuantas veces se uso para verificar que no se hashea en vano.
 */
public class FakePasswordHasher implements PasswordHasher {

    private int hashCalls;

    @Override
    public String hash(String rawPassword) {
        hashCalls++;
        return "hashed:" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordHash.equals("hashed:" + rawPassword);
    }

    public int hashCalls() {
        return hashCalls;
    }
}