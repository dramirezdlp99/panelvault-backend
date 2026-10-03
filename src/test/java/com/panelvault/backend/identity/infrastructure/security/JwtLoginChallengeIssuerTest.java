package com.panelvault.backend.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.application.IssuedChallenge;
import com.panelvault.backend.identity.domain.DisplayName;
import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Prueba el ticket del segundo paso con JWT reales. Lo mas importante: un ticket no sirve como
 * access token y un access token no sirve como ticket, aunque ambos salgan de la misma clave maestra.
 */
class JwtLoginChallengeIssuerTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("clave-de-prueba-de-48-bytes-para-hmac-sha256!!!!".getBytes(StandardCharsets.UTF_8));
    private static final String ISSUER = "http://panelvault.test";

    private final SecretKey accessKey = TokenConfiguration.signingKey(SECRET);
    private final JwtLoginChallengeIssuer challenges =
            new JwtLoginChallengeIssuer(accessKey, ISSUER, Duration.ofMinutes(5));
    private final UserId usuario = UserId.newId();

    private static String codigo(Runnable accion) {
        try {
            accion.run();
            return "sin error";
        } catch (UnauthenticatedException e) {
            return e.code();
        }
    }

    @Test
    void elTicketIdentificaAlUsuarioYVenceEnCincoMinutos() {
        Instant ahora = Instant.now();
        IssuedChallenge ticket = challenges.issue(usuario, ahora);

        assertThat(challenges.verify(ticket.token())).isEqualTo(usuario);
        // La emision se redondea al segundo, asi que faltan entre 4:59 y 5:00 minutos.
        assertThat(Duration.between(ahora, ticket.expiresAt()))
                .isBetween(Duration.ofMinutes(5).minusSeconds(1), Duration.ofMinutes(5));
    }

    @Test
    void unTicketVencidoSeRechaza() {
        IssuedChallenge viejo = challenges.issue(usuario, Instant.now().minus(Duration.ofMinutes(10)));
        assertThat(codigo(() -> challenges.verify(viejo.token()))).isEqualTo("auth.challenge_invalid");
    }

    @Test
    void unTicketAlteradoOInventadoSeRechaza() {
        String token = challenges.issue(usuario, Instant.now()).token();
        String[] partes = token.split("\\.");
        char[] firma = partes[2].toCharArray();
        firma[10] = firma[10] == 'A' ? 'B' : 'A';

        assertThat(codigo(() -> challenges.verify(partes[0] + "." + partes[1] + "." + new String(firma))))
                .isEqualTo("auth.challenge_invalid");
        assertThat(codigo(() -> challenges.verify("esto.no.sirve"))).isEqualTo("auth.challenge_invalid");
        assertThat(codigo(() -> challenges.verify(null))).isEqualTo("auth.challenge_invalid");
    }

    @Test
    void elTicketNoSirveComoAccessToken() {
        String ticket = challenges.issue(usuario, Instant.now()).token();

        assertThatThrownBy(() -> TokenConfiguration.decoder(accessKey, ISSUER).decode(ticket))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void unAccessTokenNoSirveComoTicket() {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Jwt(SECRET, ISSUER, Duration.ofMinutes(15)),
                Duration.ofDays(7),
                null,
                new SecurityProperties.Attempts(5, Duration.ofMinutes(15)));
        JwtAccessTokenIssuer accessTokens =
                new JwtAccessTokenIssuer(new TokenConfiguration().jwtEncoder(properties), properties);
        User user = User.register(new Email("mj@watson.com"), new DisplayName("Mary Jane"), "$2a$12$hash", Instant.now());
        String accessToken = accessTokens.issue(user, Instant.now()).value();

        assertThat(codigo(() -> challenges.verify(accessToken))).isEqualTo("auth.challenge_invalid");
    }
}