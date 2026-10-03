package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.TwoFactorRepository;
import com.panelvault.backend.identity.domain.TwoFactorSettings;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserRepository;
import com.panelvault.backend.shared.error.InvalidInputException;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: primer paso del login (correo y contrasena).
 *
 * <p>Defensas:
 * <ul>
 *   <li><b>Enumeracion de cuentas:</b> "el correo no existe" y "la contrasena es incorrecta" dan el
 *       mismo error, y cuando el correo no existe se compara igual contra un hash de relleno para
 *       que ambos casos tarden lo mismo (~250 ms).</li>
 *   <li><b>Fuerza bruta:</b> tras 5 fallos en 15 minutos para un mismo correo se responde 429. El
 *       limite se revisa ANTES de comparar la contrasena, asi un atacante bloqueado no gasta CPU.</li>
 *   <li><b>2FA:</b> si el usuario la activo, una contrasena correcta no basta; se entrega un ticket
 *       temporal para el segundo paso en vez de los tokens.</li>
 * </ul>
 */
@Service
public class LoginService {

    static final String INVALID_CREDENTIALS = "auth.invalid_credentials";

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final SessionTokenService sessions;
    private final TwoFactorRepository twoFactor;
    private final LoginChallengeIssuer challenges;
    private final AttemptLimiter limiter;
    private final Clock clock;
    private final String timingEqualizerHash;

    public LoginService(
            UserRepository users,
            PasswordHasher passwordHasher,
            SessionTokenService sessions,
            TwoFactorRepository twoFactor,
            LoginChallengeIssuer challenges,
            AttemptLimiter limiter,
            Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        this.twoFactor = twoFactor;
        this.challenges = challenges;
        this.limiter = limiter;
        this.clock = clock;
        // Se calcula una sola vez al arrancar: un hash real con el mismo costo que los de verdad.
        this.timingEqualizerHash = passwordHasher.hash("panelvault-timing-equalizer");
    }

    @Transactional
    public LoginResult login(String rawEmail, String rawPassword) {
        String limiterKey = "login:" + (rawEmail == null ? "" : rawEmail.strip().toLowerCase(Locale.ROOT));
        limiter.ensureAllowed(limiterKey);

        Optional<User> user = parseEmail(rawEmail).flatMap(users::findByEmail);
        String password = rawPassword == null ? "" : rawPassword;
        String hash = user.map(User::passwordHash).orElse(timingEqualizerHash);

        boolean passwordMatches = passwordHasher.matches(password, hash);
        if (user.isEmpty() || !passwordMatches) {
            limiter.recordFailure(limiterKey);
            throw new UnauthenticatedException(INVALID_CREDENTIALS, "Correo o contrasena incorrectos");
        }
        limiter.reset(limiterKey);

        User authenticated = user.get();
        boolean requiresSecondFactor = twoFactor.findByUserId(authenticated.id())
                .map(TwoFactorSettings::isEnabled)
                .orElse(false);
        if (requiresSecondFactor) {
            return new LoginResult.TwoFactorRequired(challenges.issue(authenticated.id(), clock.instant()));
        }
        return new LoginResult.Authenticated(sessions.openSession(authenticated));
    }

    private static Optional<Email> parseEmail(String rawEmail) {
        try {
            return Optional.of(new Email(rawEmail));
        } catch (InvalidInputException e) {
            return Optional.empty();
        }
    }
}