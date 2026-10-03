package com.panelvault.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.Role;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.BusinessRuleException;
import com.panelvault.backend.shared.error.NotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangeUserRoleServiceTest {

    private InMemoryUserRepository users;
    private ChangeUserRoleService service;
    private User admin;
    private User lector;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        MutableClock clock = new MutableClock(Instant.parse("2026-10-02T20:00:00Z"));
        RegisterUserService register = new RegisterUserService(users, new FakePasswordHasher(), clock);
        admin = register.register(new RegisterUserCommand("admin@panelvault.app", "Admin", "Telarana2026"));
        admin.changeRole(Role.ADMIN, clock.instant());
        users.save(admin);
        lector = register.register(new RegisterUserCommand("mj@watson.com", "Mary Jane", "Telarana2026"));
        service = new ChangeUserRoleService(users, clock);
    }

    @Test
    void unAdminPuedeCambiarElRolDeOtroUsuario() {
        User actualizado = service.changeRole(admin.id(), lector.id(), Role.CURADOR);

        assertThat(actualizado.role()).isEqualTo(Role.CURADOR);
        assertThat(users.findById(lector.id()).orElseThrow().role()).isEqualTo(Role.CURADOR);
    }

    @Test
    void unAdminNoPuedeCambiarSuPropioRol() {
        assertThatThrownBy(() -> service.changeRole(admin.id(), admin.id(), Role.LECTOR))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("user.cannot_change_own_role");
        assertThat(users.findById(admin.id()).orElseThrow().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void unUsuarioInexistenteDa404() {
        assertThatThrownBy(() -> service.changeRole(admin.id(), UserId.newId(), Role.CURADOR))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("user.not_found");
    }
}