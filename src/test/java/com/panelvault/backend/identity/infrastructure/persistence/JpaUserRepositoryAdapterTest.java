package com.panelvault.backend.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.DisplayName;
import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.Role;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.ConflictException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integracion contra PostgreSQL real (base panelvault_test).
 *
 * <p>Verifica lo que una prueba en memoria no puede: que la migracion de Flyway, el mapeo JPA y las
 * restricciones de la tabla funcionan juntos. Cada prueba corre en una transaccion que se deshace
 * al final, asi la base queda limpia.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaUserRepositoryAdapterTest {

    private static final Instant AHORA = Instant.parse("2026-10-02T20:00:00.123456Z");

    @Autowired
    private JpaUserRepositoryAdapter repository;

    private static User usuario(String correo) {
        return User.register(new Email(correo), new DisplayName("Peter Parker"), "$2a$12$hashdeprueba", AHORA);
    }

    @Test
    void guardaYRecuperaUnUsuarioConTodosSusDatos() {
        User original = usuario("peter@dailybugle.com");
        repository.save(original);

        User leido = repository.findByEmail(new Email("peter@dailybugle.com")).orElseThrow();

        assertThat(leido.id()).isEqualTo(original.id());
        assertThat(leido.email()).isEqualTo(original.email());
        assertThat(leido.displayName()).isEqualTo(original.displayName());
        assertThat(leido.passwordHash()).isEqualTo("$2a$12$hashdeprueba");
        assertThat(leido.role()).isEqualTo(Role.LECTOR);
        assertThat(leido.createdAt()).isEqualTo(AHORA);
    }

    @Test
    void buscaPorId() {
        User original = repository.save(usuario("mj@watson.com"));
        assertThat(repository.findById(original.id())).contains(original);
        assertThat(repository.findById(new UserId(UUID.randomUUID()))).isEmpty();
    }

    @Test
    void existsByEmailDistingueCorreosRegistradosDeLosQueNo() {
        repository.save(usuario("gwen@stacy.com"));
        assertThat(repository.existsByEmail(new Email("GWEN@stacy.com"))).isTrue();
        assertThat(repository.existsByEmail(new Email("miles@morales.com"))).isFalse();
    }

    @Test
    void laRestriccionUnicaDeLaBaseSeTraduceEnConflicto() {
        repository.save(usuario("harry@oscorp.com"));

        assertThatThrownBy(() -> repository.save(usuario("harry@oscorp.com")))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo("user.email_taken");
    }

    @Test
    void guardaUnCambioDeRol() {
        User user = repository.save(usuario("may@parker.com"));
        user.changeRole(Role.CURADOR, AHORA.plusSeconds(60));
        repository.save(user);

        assertThat(repository.findById(user.id()).orElseThrow().role()).isEqualTo(Role.CURADOR);
    }
}