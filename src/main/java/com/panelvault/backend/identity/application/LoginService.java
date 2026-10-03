package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserRepository;
import com.panelvault.backend.shared.error.InvalidInputException;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: iniciar sesion con correo y contrasena.
 *
 * <p>Defensa contra la enumeracion de cuentas: tanto "el correo no existe" como "la contrasena es
 * incorrecta" responden con el mismo codigo y mensaje. Ademas, cuando el correo no existe se
 * compara igual la contrasena contra un hash de relleno, para que ambos casos tarden lo mismo
 * (~250 ms). Sin esto, un atacante podria distinguirlos midiendo el tiempo de respuesta.
 */
@Service
public class LoginService {

    static final String INVALID_CREDENTIALS = "auth.invalid_credentials";

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final SessionTokenService sessions;
    private final String timingEqualizerHash;

    public LoginService(UserRepository users, PasswordHasher passwordHasher, SessionTokenService sessions) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.sessions = sessions;
        // Se calcula una sola vez al arrancar: un hash real con el mismo costo que los de verdad.
        this.timingEqualizerHash = passwordHasher.hash("panelvault-timing-equalizer");
    }

    @Transactional
    public AuthTokens login(String rawEmail, String rawPassword) {
        Optional<User> user = parseEmail(rawEmail).flatMap(users::findByEmail);
        String password = rawPassword == null ? "" : rawPassword;
        String hash = user.map(User::passwordHash).orElse(timingEqualizerHash);

        boolean passwordMatches = passwordHasher.matches(password, hash);
        if (user.isEmpty() || !passwordMatches) {
            throw new UnauthenticatedException(INVALID_CREDENTIALS, "Correo o contrasena incorrectos");
        }
        return sessions.openSession(user.get());
    }

    private static Optional<Email> parseEmail(String rawEmail) {
        try {
            return Optional.of(new Email(rawEmail));
        } catch (InvalidInputException e) {
            return Optional.empty();
        }
    }
}