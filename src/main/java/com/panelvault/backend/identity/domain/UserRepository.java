package com.panelvault.backend.identity.domain;

import java.util.Optional;

/**
 * Puerto de salida para guardar y buscar usuarios.
 *
 * <p>El dominio define QUE necesita; la infraestructura (JPA + PostgreSQL) decide COMO. En las
 * pruebas se reemplaza por una version en memoria sin tocar la base de datos.
 */
public interface UserRepository {

    boolean existsByEmail(Email email);

    Optional<User> findByEmail(Email email);

    Optional<User> findById(UserId id);

    /**
     * Guarda el usuario (crea o actualiza).
     *
     * @throws com.panelvault.backend.shared.error.ConflictException si el correo ya esta en uso
     */
    User save(User user);
}