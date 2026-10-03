package com.panelvault.backend.identity.infrastructure.persistence;

import com.panelvault.backend.identity.domain.TwoFactorRepository;
import com.panelvault.backend.identity.domain.TwoFactorSettings;
import com.panelvault.backend.identity.domain.UserId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador que implementa {@link TwoFactorRepository} con JPA y PostgreSQL. */
@Repository
public class JpaTwoFactorRepositoryAdapter implements TwoFactorRepository {

    private static final String SEPARATOR = ",";

    private final SpringDataTwoFactorRepository jpa;

    public JpaTwoFactorRepositoryAdapter(SpringDataTwoFactorRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<TwoFactorSettings> findByUserId(UserId userId) {
        return jpa.findById(userId.value()).map(JpaTwoFactorRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<TwoFactorSettings> findByUserIdForUpdate(UserId userId) {
        return jpa.findByUserIdForUpdate(userId.value()).map(JpaTwoFactorRepositoryAdapter::toDomain);
    }

    @Override
    public TwoFactorSettings save(TwoFactorSettings settings) {
        return toDomain(jpa.saveAndFlush(toEntity(settings)));
    }

    @Override
    public void delete(UserId userId) {
        jpa.deleteById(userId.value());
        jpa.flush();
    }

    private static TwoFactorJpaEntity toEntity(TwoFactorSettings settings) {
        return new TwoFactorJpaEntity(
                settings.userId().value(),
                settings.encryptedSecret(),
                String.join(SEPARATOR, settings.recoveryCodeFingerprints()),
                settings.lastUsedTimeStep(),
                settings.createdAt(),
                settings.enabledAt());
    }

    private static TwoFactorSettings toDomain(TwoFactorJpaEntity row) {
        List<String> fingerprints = row.getRecoveryCodes().isEmpty()
                ? List.of()
                : Arrays.asList(row.getRecoveryCodes().split(SEPARATOR));
        return TwoFactorSettings.rehydrate(
                new UserId(row.getUserId()),
                row.getSecretCiphertext(),
                row.getCreatedAt(),
                row.getEnabledAt(),
                row.getLastUsedStep(),
                fingerprints);
    }
}