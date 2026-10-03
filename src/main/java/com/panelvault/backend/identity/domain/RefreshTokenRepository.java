package com.panelvault.backend.identity.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida para los refresh tokens. */
public interface RefreshTokenRepository {

    /**
     * Busca un token por su hash y lo bloquea hasta el final de la transaccion.
     *
     * <p>El bloqueo evita una carrera: si dos peticiones llegan al mismo tiempo con el mismo token,
     * la segunda espera a que la primera termine y entonces lo ve ya revocado, en vez de rotarlo
     * dos veces.
     */
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    RefreshToken save(RefreshToken token);

    /** Revoca todos los tokens aun activos de una familia. Devuelve cuantos revoco. */
    int revokeFamily(UUID familyId, Instant now);
}