package com.panelvault.backend.identity.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorio de Spring Data para {@link TwoFactorJpaEntity}. */
public interface SpringDataTwoFactorRepository extends JpaRepository<TwoFactorJpaEntity, UUID> {

    /** {@code SELECT ... FOR UPDATE}: bloquea la fila hasta el fin de la transaccion. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TwoFactorJpaEntity t where t.userId = :userId")
    Optional<TwoFactorJpaEntity> findByUserIdForUpdate(@Param("userId") UUID userId);
}