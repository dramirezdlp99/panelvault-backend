package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.shared.crypto.Base32;
import com.panelvault.backend.shared.crypto.Totp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Arma todo el modulo identity en memoria para las pruebas (Test Fixture): repositorios falsos,
 * reloj controlable y los casos de uso reales conectados entre si, igual que en produccion.
 *
 * <p>Asi cada prueba se enfoca en su escenario y no en como se construyen los objetos.
 */
public class AuthFixture {

    public static final Instant START = Instant.parse("2026-10-02T20:00:00Z");
    public static final String PASSWORD = "Telarana2026";

    public final MutableClock clock = new MutableClock(START);
    public final InMemoryUserRepository users = new InMemoryUserRepository();
    public final InMemoryRefreshTokenRepository refreshTokens = new InMemoryRefreshTokenRepository();
    public final InMemoryTwoFactorRepository twoFactor = new InMemoryTwoFactorRepository();
    public final FakePasswordHasher hasher = new FakePasswordHasher();
    public final FakeSecretProtector protector = new FakeSecretProtector();
    public final FakeLoginChallengeIssuer challenges = new FakeLoginChallengeIssuer();
    public final AttemptLimiter limiter = new AttemptLimiter(5, Duration.ofMinutes(15), clock);
    public final SessionTokenService sessions = new SessionTokenService(
            refreshTokens, new FakeAccessTokenIssuer(), new OpaqueTokenGenerator(), clock, Duration.ofDays(7));
    public final TwoFactorCodeChecker checker = new TwoFactorCodeChecker(protector, clock);

    public final RegisterUserService register = new RegisterUserService(users, hasher, clock);
    public final LoginService login = new LoginService(users, hasher, sessions, twoFactor, challenges, limiter, clock);
    public final TwoFactorLoginService twoFactorLogin =
            new TwoFactorLoginService(challenges, twoFactor, users, checker, sessions, limiter);
    public final TwoFactorSetupService twoFactorSetup = new TwoFactorSetupService(
            twoFactor, users, protector, checker, new RecoveryCodes(), limiter, clock,
            new TwoFactorIssuerLabel("PanelVault"));
    public final TokenRefreshService refresh = new TokenRefreshService(refreshTokens, users, sessions, clock);

    /** Resultado de activar la 2FA: el secreto (como lo tendria la app del usuario) y sus codigos. */
    public record Activation(byte[] secret, List<String> recoveryCodes) {}

    public User registrar(String email) {
        return register.register(new RegisterUserCommand(email, "Usuario Prueba", PASSWORD));
    }

    /** Login de un usuario SIN 2FA; falla si el resultado no es una sesion abierta. */
    public AuthTokens iniciarSesion(String email) {
        LoginResult result = login.login(email, PASSWORD);
        if (result instanceof LoginResult.Authenticated authenticated) {
            return authenticated.tokens();
        }
        throw new AssertionError("Se esperaba una sesion abierta y se pidio el segundo paso");
    }

    /** Activa la 2FA como lo haria el usuario: escanea el QR y confirma con el codigo actual. */
    public Activation activarDosPasos(User user) {
        byte[] secret = Base32.decode(twoFactorSetup.begin(user.id()).secretBase32());
        List<String> codes = twoFactorSetup.confirm(user.id(), codigoActual(secret));
        return new Activation(secret, codes);
    }

    /** Lo que muestra la app autenticadora en este momento. */
    public String codigoActual(byte[] secret) {
        return Totp.code(secret, Totp.timeStep(clock.instant()));
    }

    /** Espera al siguiente intervalo de 30 s y devuelve el codigo nuevo. */
    public String codigoSiguiente(byte[] secret) {
        clock.advance(Duration.ofSeconds(Totp.PERIOD_SECONDS));
        return codigoActual(secret);
    }
}