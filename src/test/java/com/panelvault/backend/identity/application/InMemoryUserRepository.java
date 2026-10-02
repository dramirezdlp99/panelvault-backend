package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.identity.domain.UserRepository;
import com.panelvault.backend.shared.error.ConflictException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementacion en memoria del puerto {@link UserRepository} para pruebas (Fake).
 *
 * <p>Respeta la misma regla que la base: no admite dos usuarios con el mismo correo. Asi las
 * pruebas de la capa de aplicacion no necesitan PostgreSQL y corren en milisegundos.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<UserId, User> users = new LinkedHashMap<>();

    @Override
    public boolean existsByEmail(Email email) {
        return users.values().stream().anyMatch(u -> u.email().equals(email));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return users.values().stream().filter(u -> u.email().equals(email)).findFirst();
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public User save(User user) {
        boolean emailTakenByOther = users.values().stream()
                .anyMatch(u -> u.email().equals(user.email()) && !u.id().equals(user.id()));
        if (emailTakenByOther) {
            throw new ConflictException("user.email_taken", "Ya existe una cuenta con ese correo");
        }
        users.put(user.id(), user);
        return user;
    }

    public int count() {
        return users.size();
    }
}