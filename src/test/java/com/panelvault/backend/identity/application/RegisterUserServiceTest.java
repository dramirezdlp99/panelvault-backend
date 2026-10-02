package com.panelvault.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.Role;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.shared.error.ConflictException;
import com.panelvault.backend.shared.error.InvalidInputException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegisterUserServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-02T20:00:00Z");

    private InMemoryUserRepository users;
    private FakePasswordHasher hasher;
    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        hasher = new FakePasswordHasher();
        service = new RegisterUserService(users, hasher, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void registraUnaCuentaNuevaComoLectorConLaContrasenaHasheada() {
        User user = service.register(new RegisterUserCommand("Peter@DailyBugle.com", "Peter Parker", "Telarana2026"));

        assertThat(user.email().value()).isEqualTo("peter@dailybugle.com");
        assertThat(user.displayName().value()).isEqualTo("Peter Parker");
        assertThat(user.role()).isEqualTo(Role.LECTOR);
        assertThat(user.passwordHash()).isEqualTo("hashed:Telarana2026");
        assertThat(user.createdAt()).isEqualTo(AHORA);
        assertThat(users.findByEmail(new Email("peter@dailybugle.com"))).contains(user);
    }

    @Test
    void rechazaUnCorreoYaRegistradoAunqueCambienLasMayusculas() {
        service.register(new RegisterUserCommand("peter@dailybugle.com", "Peter", "Telarana2026"));

        assertThatThrownBy(() -> service.register(
                new RegisterUserCommand("PETER@dailybugle.com", "Otro Peter", "OtraClave2026")))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo("user.email_taken");
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void unaContrasenaDebilNoLlegaANingunLado() {
        assertThatThrownBy(() -> service.register(new RegisterUserCommand("mj@watson.com", "Mary Jane", "corta")))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("user.weak_password");
        assertThat(users.count()).isZero();
        assertThat(hasher.hashCalls()).isZero();
    }

    @Test
    void noSeCalculaElHashSiElCorreoYaExiste() {
        service.register(new RegisterUserCommand("mj@watson.com", "Mary Jane", "Telarana2026"));
        int antes = hasher.hashCalls();

        assertThatThrownBy(() -> service.register(new RegisterUserCommand("mj@watson.com", "MJ", "Telarana2026")))
                .isInstanceOf(ConflictException.class);
        assertThat(hasher.hashCalls()).isEqualTo(antes);
    }

    @Test
    void unCorreoInvalidoSeRechazaAntesDeTocarElRepositorio() {
        assertThatThrownBy(() -> service.register(new RegisterUserCommand("no-es-correo", "Alguien", "Telarana2026")))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("user.email_invalid");
    }

    @Test
    void elComandoNoMuestraLaContrasenaEnSuToString() {
        assertThat(new RegisterUserCommand("a@b.co", "Ab", "Telarana2026").toString())
                .doesNotContain("Telarana2026");
    }
}