package com.panelvault.backend.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.application.IssuedAccessToken;
import com.panelvault.backend.identity.domain.DisplayName;
import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Prueba la firma y verificacion real de JWT (Nimbus), sin levantar Spring.
 *
 * <p>Usa {@code Instant.now()} y no un reloj fijo porque el decodificador valida el vencimiento
 * contra la hora real.
 */
class JwtAccessTokenIssuerTest {

    private static final String SECRET = base64("clave-de-prueba-de-48-bytes-para-hmac-sha256!!!!");
    private static final String ISSUER = "http://panelvault.test";

    private final SecurityProperties properties = new SecurityProperties(
            new SecurityProperties.Jwt(SECRET, ISSUER, Duration.ofMinutes(15)), Duration.ofDays(7));
    private final TokenConfiguration configuration = new TokenConfiguration();
    private final JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(configuration.jwtEncoder(properties), properties);
    private final JwtDecoder decoder = configuration.jwtDecoder(properties);
    private final User peter = User.register(
            new Email("peter@dailybugle.com"), new DisplayName("Peter Parker"), "$2a$12$hash", Instant.now());

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void emiteUnJwtConLosClaimsEsperados() {
        IssuedAccessToken token = issuer.issue(peter, Instant.now());

        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo(peter.id().toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("LECTOR");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Peter Parker");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(jwt.getId()).isNotBlank();
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
        assertThat(String.valueOf(jwt.getHeaders().get("alg"))).isEqualTo("HS256");
    }

    @Test
    void elTokenNoLlevaElCorreo() {
        String payload = issuer.issue(peter, Instant.now()).value().split("\\.")[1];
        String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        assertThat(json).doesNotContain("dailybugle");
    }

    @Test
    void unaFirmaAlteradaSeRechaza() {
        String token = issuer.issue(peter, Instant.now()).value();
        String[] parts = token.split("\\.");
        char[] firma = parts[2].toCharArray();
        firma[10] = firma[10] == 'A' ? 'B' : 'A';
        String alterado = parts[0] + "." + parts[1] + "." + new String(firma);

        assertThatThrownBy(() -> decoder.decode(alterado)).isInstanceOf(JwtException.class);
    }

    @Test
    void otraClaveNoPuedeVerificarlo() {
        String token = issuer.issue(peter, Instant.now()).value();
        JwtDecoder otro = TokenConfiguration.decoder(
                TokenConfiguration.signingKey(base64("otra-clave-distinta-de-48-bytes-para-hmac-256!!!")), ISSUER);

        assertThatThrownBy(() -> otro.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenVencidoSeRechaza() {
        String token = issuer.issue(peter, Instant.now().minus(Duration.ofHours(1))).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenDeOtroEmisorSeRechaza() {
        String token = issuer.issue(peter, Instant.now()).value();
        JwtDecoder otroEmisor = TokenConfiguration.decoder(TokenConfiguration.signingKey(SECRET), "http://otro.test");

        assertThatThrownBy(() -> otroEmisor.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void exigeUnSecretoBase64DeAlMenos32Bytes() {
        assertThatThrownBy(() -> TokenConfiguration.signingKey(base64("muy-corta")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("al menos 32 bytes");
        assertThatThrownBy(() -> TokenConfiguration.signingKey("esto no es base64 !!"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> TokenConfiguration.signingKey(null)).isInstanceOf(IllegalStateException.class);
    }
}