package com.panelvault.backend.identity.domain;

import java.util.Optional;

/** Puerto de salida para la configuracion de verificacion en dos pasos. */
public interface TwoFactorRepository {

    Optional<TwoFactorSettings> findByUserId(UserId userId);

    /**
     * Igual que {@link #findByUserId} pero bloquea la fila hasta el fin de la transaccion. Evita que
     * dos peticiones simultaneas acepten el mismo codigo antes de que alguna guarde el cambio.
     */
    Optional<TwoFactorSettings> findByUserIdForUpdate(UserId userId);

    TwoFactorSettings save(TwoFactorSettings settings);

    void delete(UserId userId);
}