package com.panelvault.backend.identity.infrastructure.security;

import com.panelvault.backend.identity.application.IssuedChallenge;
import com.panelvault.backend.identity.application.LoginChallengeIssuer;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Emite el ticket del segundo paso como un JWT de vida corta (5 minutos).
 *
 * <p>Se firma con una <b>clave distinta</b> a la de los access tokens, derivada de la misma clave
 * maestra. Asi el ticket nunca sirve como access token (la API rechaza su firma) y un access token
 * nunca sirve como ticket. Ademas lleva la audiencia {@code panelvault-2fa}.
 *
 * <p>No se registra como bean {@code JwtDecoder}: si hubiera dos, Spring Security no sabria cual
 * usar para validar las peticiones normales.
 */
public class JwtLoginChallengeIssuer implements LoginChallengeIssuer {

    static final String AUDIENCE = "panelvault-2fa";

    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final String issuer;
    private final Duration ttl;

    public JwtLoginChallengeIssuer(SecretKey accessTokenKey, String issuer, Duration ttl) {
        SecretKey challengeKey = new SecretKeySpec(
                AesGcmSecretProtector.hmac(accessTokenKey.getEncoded(), "panelvault-2fa-challenge"), "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(challengeKey).algorithm(MacAlgorithm.HS256).build();
        this.decoder = NimbusJwtDecoder.withSecretKey(challengeKey).macAlgorithm(MacAlgorithm.HS256).build();
        this.decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator(issuer), new JwtAudienceValidator(AUDIENCE)));
        this.issuer = issuer;
        this.ttl = ttl;
    }

    @Override
    public IssuedChallenge issue(UserId userId, Instant now) {
        Instant issuedAt = now.truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .audience(List.of(AUDIENCE))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .build();
        return new IssuedChallenge(encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue(), expiresAt);
    }

    @Override
    public UserId verify(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            return new UserId(UUID.fromString(jwt.getSubject()));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new UnauthenticatedException(
                    "auth.challenge_invalid", "La verificacion expiro. Inicia sesion de nuevo");
        }
    }
}