package com.panelvault.backend.identity.infrastructure.persistence;

import com.panelvault.backend.identity.domain.RefreshToken;
import com.panelvault.backend.identity.domain.RefreshTokenRepository;
import com.panelvault.backend.identity.domain.UserId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptador que implementa {@link RefreshTokenRepository} con JPA y PostgreSQL. */
@Repository
public class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository jpa;

    public JpaRefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
        return jpa.findByTokenHashForUpdate(tokenHash).map(JpaRefreshTokenRepositoryAdapter::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return toDomain(jpa.saveAndFlush(toEntity(token)));
    }

    @Override
    public int revokeFamily(UUID familyId, Instant now) {
        return jpa.revokeFamily(familyId, now.truncatedTo(ChronoUnit.MICROS));
    }

    private static RefreshTokenJpaEntity toEntity(RefreshToken token) {
        return new RefreshTokenJpaEntity(
                token.id(),
                token.userId().value(),
                token.familyId(),
                token.tokenHash(),
                token.issuedAt(),
                token.expiresAt(),
                token.revokedAt(),
                token.replacedBy());
    }

    private static RefreshToken toDomain(RefreshTokenJpaEntity row) {
        return RefreshToken.rehydrate(
                row.getId(),
                new UserId(row.getUserId()),
                row.getFamilyId(),
                row.getTokenHash(),
                row.getIssuedAt(),
                row.getExpiresAt(),
                row.getRevokedAt(),
                row.getReplacedBy());
    }
}