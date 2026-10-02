package com.panelvault.backend.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de Spring Data. Spring genera la implementacion a partir del nombre de cada metodo
 * (por ejemplo, {@code existsByEmail} produce un SELECT con WHERE email = ?).
 *
 * <p>Solo lo usa {@link JpaUserRepositoryAdapter}; el resto del sistema habla con el puerto.
 */
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByEmail(String email);
}