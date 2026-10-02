package com.panelvault.backend.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant AHORA = Instant.parse("2026-10-02T20:00:00.123456789Z");

    private User nuevo() {
        return User.register(new Email("mj@watson.com"), new DisplayName("Mary Jane"), "$2a$12$hash", AHORA);
    }

    @Test
    void todaCuentaNuevaNaceComoLector() {
        assertThat(nuevo().role()).isEqualTo(Role.LECTOR);
    }

    @Test
    void lasFechasSeTruncanAMicrosegundosComoEnPostgres() {
        User user = nuevo();
        assertThat(user.createdAt()).isEqualTo(Instant.parse("2026-10-02T20:00:00.123456Z"));
        assertThat(user.updatedAt()).isEqualTo(user.createdAt());
    }

    @Test
    void cadaRegistroGeneraUnIdDistinto() {
        assertThat(nuevo().id()).isNotEqualTo(nuevo().id());
    }

    @Test
    void cambiarElRolActualizaLaFechaDeModificacion() {
        User user = nuevo();
        Instant despues = AHORA.plusSeconds(60);
        user.changeRole(Role.CURADOR, despues);
        assertThat(user.role()).isEqualTo(Role.CURADOR);
        assertThat(user.updatedAt()).isEqualTo(despues.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    }

    @Test
    void asignarElMismoRolNoCambiaNada() {
        User user = nuevo();
        user.changeRole(Role.LECTOR, AHORA.plusSeconds(60));
        assertThat(user.updatedAt()).isEqualTo(user.createdAt());
    }

    @Test
    void laIgualdadEsPorId() {
        User user = nuevo();
        User mismo = User.rehydrate(user.id(), new Email("otro@mail.com"), new DisplayName("Otro"),
                "$2a$12$otro", Role.ADMIN, AHORA, AHORA);
        assertThat(user).isEqualTo(mismo).hasSameHashCodeAs(mismo);
    }

    @Test
    void elToStringNuncaMuestraElHash() {
        assertThat(nuevo().toString()).doesNotContain("$2a$");
    }

    @Test
    void exigeUnHashDeContrasena() {
        assertThatThrownBy(() -> User.register(new Email("a@b.co"), new DisplayName("Ab"), " ", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }
}