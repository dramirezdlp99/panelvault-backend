package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.TwoFactorRepository;
import com.panelvault.backend.identity.domain.TwoFactorSettings;
import com.panelvault.backend.identity.domain.UserId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementacion en memoria de {@link TwoFactorRepository} para pruebas (Fake). Guarda copias, como
 * una base de datos: los cambios solo cuentan despues de {@link #save}.
 */
public class InMemoryTwoFactorRepository implements TwoFactorRepository {

    private final Map<UserId, TwoFactorSettings> settings = new HashMap<>();

    @Override
    public Optional<TwoFactorSettings> findByUserId(UserId userId) {
        return Optional.ofNullable(settings.get(userId)).map(InMemoryTwoFactorRepository::copy);
    }

    @Override
    public Optional<TwoFactorSettings> findByUserIdForUpdate(UserId userId) {
        return findByUserId(userId);
    }

    @Override
    public TwoFactorSettings save(TwoFactorSettings value) {
        settings.put(value.userId(), copy(value));
        return value;
    }

    @Override
    public void delete(UserId userId) {
        settings.remove(userId);
    }

    private static TwoFactorSettings copy(TwoFactorSettings s) {
        return TwoFactorSettings.rehydrate(s.userId(), s.encryptedSecret(), s.createdAt(), s.enabledAt(),
                s.lastUsedTimeStep(), s.recoveryCodeFingerprints());
    }
}