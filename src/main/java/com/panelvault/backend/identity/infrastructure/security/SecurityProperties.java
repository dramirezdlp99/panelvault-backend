package com.panelvault.backend.identity.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades {@code panelvault.security.*} de application.yaml.
 *
 * <p>Las claves llegan por variables de entorno ({@code PANELVAULT_JWT_SECRET} y
 * {@code PANELVAULT_TOTP_ENCRYPTION_KEY}); si falta alguna, la aplicacion no arranca (es preferible
 * a arrancar con una clave inventada).
 */
@ConfigurationProperties(prefix = "panelvault.security")
public record SecurityProperties(Jwt jwt, Duration refreshTokenTtl, TwoFactor twoFactor, Attempts attempts) {

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

    /**
     * @param encryptionKey clave AES-256 en Base64 (exactamente 32 bytes) para cifrar secretos TOTP
     * @param issuerLabel   nombre que se muestra en la app autenticadora
     * @param challengeTtl  cuanto dura el ticket entre el primer y el segundo paso del login
     */
    public record TwoFactor(String encryptionKey, String issuerLabel, Duration challengeTtl) {

        @Override
        public String toString() {
            return "TwoFactor[issuerLabel=" + issuerLabel + ", challengeTtl=" + challengeTtl + ", encryptionKey=***]";
        }
    }

    /**
     * @param maxFailures fallos permitidos dentro de la ventana antes de bloquear
     * @param window      duracion de la ventana deslizante
     */
    public record Attempts(int maxFailures, Duration window) {}
}