package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.RefreshToken;
import com.panelvault.backend.identity.domain.RefreshTokenRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementacion en memoria de {@link RefreshTokenRepository} para pruebas (Fake).
 *
 * <p>Guarda copias, como haria una base de datos: modificar el objeto devuelto no cambia lo
 * guardado hasta llamar a {@link #save}.
 */
public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<UUID, RefreshToken> tokens = new LinkedHashMap<>();

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
        return tokens.values().stream()
                .filter(t -> t.tokenHash().equals(tokenHash))
                .findFirst()
                .map(InMemoryRefreshTokenRepository::copy);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        tokens.put(token.id(), copy(token));
        return token;
    }

    @Override
    public int revokeFamily(UUID familyId, Instant now) {
        int revoked = 0;
        for (RefreshToken token : new ArrayList<>(tokens.values())) {
            if (token.familyId().equals(familyId) && !token.isRevoked()) {
                tokens.put(token.id(), RefreshToken.rehydrate(token.id(), token.userId(), token.familyId(),
                        token.tokenHash(), token.issuedAt(), token.expiresAt(), now, token.replacedBy()));
                revoked++;
            }
        }
        return revoked;
    }

    public List<RefreshToken> all() {
        return tokens.values().stream().map(InMemoryRefreshTokenRepository::copy).toList();
    }

    private static RefreshToken copy(RefreshToken t) {
        return RefreshToken.rehydrate(t.id(), t.userId(), t.familyId(), t.tokenHash(), t.issuedAt(),
                t.expiresAt(), t.revokedAt(), t.replacedBy());
    }
}