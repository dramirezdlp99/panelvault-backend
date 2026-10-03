package com.panelvault.backend.identity.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades {@code panelvault.security.*} de application.yaml.
 *
 * <p>El secreto del JWT llega por la variable de entorno {@code PANELVAULT_JWT_SECRET}; si falta,
 * la aplicacion no arranca (es preferible a arrancar con una clave inventada).
 */
@ConfigurationProperties(prefix = "panelvault.security")
public record SecurityProperties(Jwt jwt, Duration refreshTokenTtl) {

    /**
     * @param secret         clave HMAC en Base64 (minimo 32 bytes decodificados)
     * @param issuer         quien emite el token (claim {@code iss}); debe ser una URL
     * @param accessTokenTtl vida del access token
     */
    public record Jwt(String secret, String issuer, Duration accessTokenTtl) {

        @Override
        public String toString() {
            return "Jwt[issuer=" + issuer + ", accessTokenTtl=" + accessTokenTtl + ", secret=***]";
        }
    }
}