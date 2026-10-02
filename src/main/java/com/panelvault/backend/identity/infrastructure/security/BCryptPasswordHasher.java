package com.panelvault.backend.identity.infrastructure.security;

import com.panelvault.backend.identity.application.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adaptador de {@link PasswordHasher} con BCrypt.
 *
 * <p>BCrypt es lento a proposito y genera una sal aleatoria por contrasena, que queda guardada
 * dentro del propio hash ({@code $2a$12$<sal><hash>}). El factor 12 significa 2^12 rondas: unos
 * 250 ms por intento, imperceptible para un usuario y muy costoso para un ataque de fuerza bruta.
 *
 * <p>Solo se usa {@code spring-security-crypto}, no Spring Security completo: este modulo aun no
 * protege rutas. Eso llega con el login y los JWT.
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    static final int STRENGTH = 12;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(STRENGTH);

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }
}