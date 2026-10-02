package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.DisplayName;
import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.PasswordPolicy;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserRepository;
import com.panelvault.backend.shared.error.ConflictException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: registrar una cuenta nueva.
 *
 * <p>Orden deliberado: primero se valida todo lo barato (formato, politica), despues se consulta la
 * base y al final se calcula el hash, que es lo caro (BCrypt tarda ~250 ms a proposito).
 *
 * <p>La verificacion {@code existsByEmail} da un mensaje claro en el caso normal; la restriccion
 * UNIQUE de la base cubre la carrera de dos registros simultaneos con el mismo correo.
 */
@Service
public class RegisterUserService {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    public RegisterUserService(UserRepository users, PasswordHasher passwordHasher, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Transactional
    public User register(RegisterUserCommand command) {
        Email email = new Email(command.email());
        DisplayName displayName = new DisplayName(command.displayName());
        passwordPolicy.check(command.password(), email);

        if (users.existsByEmail(email)) {
            throw new ConflictException("user.email_taken", "Ya existe una cuenta con ese correo");
        }

        String passwordHash = passwordHasher.hash(command.password());
        User user = User.register(email, displayName, passwordHash, clock.instant());
        return users.save(user);
    }
}