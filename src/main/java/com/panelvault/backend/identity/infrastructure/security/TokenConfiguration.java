package com.panelvault.backend.identity.infrastructure.security;

import com.panelvault.backend.identity.application.AccessTokenIssuer;
import com.panelvault.backend.identity.application.OpaqueTokenGenerator;
import com.panelvault.backend.identity.application.SessionTokenService;
import com.panelvault.backend.identity.domain.RefreshTokenRepository;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Configuracion de los tokens: firma y verificacion de JWT, y emision de sesiones.
 *
 * <p>Se usa HS256 (HMAC-SHA256) con una clave simetrica: el mismo backend firma y verifica, asi que
 * no hace falta un par de claves publica/privada. El decodificador valida firma, vencimiento
 * ({@code exp}, con 60 s de tolerancia por desfase de relojes) y emisor ({@code iss}).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
public class TokenConfiguration {

    /** HMAC-SHA256 necesita una clave de al menos 256 bits. */
    static final int MIN_SECRET_BYTES = 32;

    @Bean
    public JwtEncoder jwtEncoder(SecurityProperties properties) {
        return NimbusJwtEncoder.withSecretKey(signingKey(properties.jwt().secret()))
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecurityProperties properties) {
        return decoder(signingKey(properties.jwt().secret()), properties.jwt().issuer());
    }

    @Bean
    public OpaqueTokenGenerator opaqueTokenGenerator() {
        return new OpaqueTokenGenerator();
    }

    @Bean
    public SessionTokenService sessionTokenService(
            RefreshTokenRepository refreshTokens,
            AccessTokenIssuer accessTokens,
            OpaqueTokenGenerator generator,
            Clock clock,
            SecurityProperties properties) {
        return new SessionTokenService(refreshTokens, accessTokens, generator, clock, properties.refreshTokenTtl());
    }

    static JwtDecoder decoder(SecretKey key, String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    /** Convierte el secreto Base64 en una clave HMAC, exigiendo el tamano minimo. */
    static SecretKey signingKey(String base64Secret) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException("Falta el secreto del JWT (variable PANELVAULT_JWT_SECRET)");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Secret.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("PANELVAULT_JWT_SECRET debe estar en Base64", e);
        }
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "PANELVAULT_JWT_SECRET debe tener al menos " + MIN_SECRET_BYTES + " bytes; tiene " + bytes.length);
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}