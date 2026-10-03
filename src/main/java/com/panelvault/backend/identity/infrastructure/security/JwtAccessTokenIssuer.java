package com.panelvault.backend.identity.infrastructure.security;

import com.panelvault.backend.identity.application.AccessTokenIssuer;
import com.panelvault.backend.identity.application.IssuedAccessToken;
import com.panelvault.backend.identity.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite access tokens como JWT firmados.
 *
 * <p>Claims que lleva cada token:
 * <ul>
 *   <li>{@code sub}: id del usuario. Es lo que el backend usa para saber quien llama.</li>
 *   <li>{@code role}: rol (LECTOR, CURADOR, ADMIN). Spring lo convierte en ROLE_*.</li>
 *   <li>{@code name}: nombre visible, para que el frontend lo muestre sin otra consulta.</li>
 *   <li>{@code iss}, {@code iat}, {@code exp}, {@code jti}: emisor, emision, vencimiento e id unico.</li>
 * </ul>
 * Nunca lleva el correo ni datos sensibles: un JWT esta firmado, no cifrado, y cualquiera puede
 * leer su contenido decodificando el Base64.
 */
@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    static final String ROLE_CLAIM = "role";
    static final String NAME_CLAIM = "name";

    private final JwtEncoder encoder;
    private final SecurityProperties.Jwt settings;

    public JwtAccessTokenIssuer(JwtEncoder encoder, SecurityProperties properties) {
        this.encoder = encoder;
        this.settings = properties.jwt();
    }

    @Override
    public IssuedAccessToken issue(User user, Instant now) {
        // El JWT guarda segundos enteros; se trunca para que el vencimiento informado coincida.
        Instant issuedAt = now.truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(settings.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(settings.issuer())
                .subject(user.id().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim(ROLE_CLAIM, user.role().name())
                .claim(NAME_CLAIM, user.displayName().value())
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new IssuedAccessToken(value, expiresAt);
    }
}