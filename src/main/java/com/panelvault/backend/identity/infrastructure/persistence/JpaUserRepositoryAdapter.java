package com.panelvault.backend.identity.infrastructure.persistence;

import com.panelvault.backend.identity.domain.DisplayName;
import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.Role;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.identity.domain.UserRepository;
import com.panelvault.backend.shared.error.ConflictException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/**
 * Adaptador que implementa el puerto {@link UserRepository} con JPA y PostgreSQL.
 *
 * <p>Traduce entre el dominio y la fila, y convierte la violacion del indice unico del correo en
 * un {@link ConflictException} (409). Se usa {@code saveAndFlush} para que esa violacion ocurra
 * aqui mismo y no mas tarde, al cerrar la transaccion, donde ya no se podria traducir.
 */
@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    /** Nombre de la restriccion definida en V1__create_users_table.sql. */
    static final String EMAIL_UNIQUE_CONSTRAINT = "uq_users_email";

    private final SpringDataUserRepository jpa;

    public JpaUserRepositoryAdapter(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(JpaUserRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value()).map(JpaUserRepositoryAdapter::toDomain);
    }

    @Override
    public User save(User user) {
        try {
            return toDomain(jpa.saveAndFlush(toEntity(user)));
        } catch (DataIntegrityViolationException e) {
            if (isEmailUniqueViolation(e)) {
                throw new ConflictException("user.email_taken", "Ya existe una cuenta con ese correo");
            }
            throw e;
        }
    }

    private static boolean isEmailUniqueViolation(DataIntegrityViolationException e) {
        String detail = e.getMostSpecificCause().getMessage();
        return detail != null && detail.contains(EMAIL_UNIQUE_CONSTRAINT);
    }

    private static UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.id().value(),
                user.email().value(),
                user.displayName().value(),
                user.passwordHash(),
                user.role().name(),
                user.createdAt(),
                user.updatedAt());
    }

    private static User toDomain(UserJpaEntity row) {
        return User.rehydrate(
                new UserId(row.getId()),
                new Email(row.getEmail()),
                new DisplayName(row.getDisplayName()),
                row.getPasswordHash(),
                Role.valueOf(row.getRole()),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}