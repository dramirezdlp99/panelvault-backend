package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.TwoFactorRepository;
import com.panelvault.backend.identity.domain.TwoFactorSettings;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.identity.domain.UserRepository;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: segundo paso del login. Recibe el ticket del primer paso y el codigo de la app (o un
 * codigo de recuperacion) y, si son validos, abre la sesion.
 *
 * <p>Tambien tiene limite de intentos: con 6 digitos hay un millon de combinaciones, y sin limite
 * se podrian probar todas antes de que venza el ticket.
 */
@Service
public class TwoFactorLoginService {

    private final LoginChallengeIssuer challenges;
    private final TwoFactorRepository twoFactor;
    private final UserRepository users;
    private final TwoFactorCodeChecker checker;
    private final SessionTokenService sessions;
    private final AttemptLimiter limiter;

    public TwoFactorLoginService(
            LoginChallengeIssuer challenges,
            TwoFactorRepository twoFactor,
            UserRepository users,
            TwoFactorCodeChecker checker,
            SessionTokenService sessions,
            AttemptLimiter limiter) {
        this.challenges = challenges;
        this.twoFactor = twoFactor;
        this.users = users;
        this.checker = checker;
        this.sessions = sessions;
        this.limiter = limiter;
    }

    @Transactional
    public AuthTokens verify(String challengeToken, String code) {
        UserId userId = challenges.verify(challengeToken);
        String limiterKey = "2fa:" + userId;
        limiter.ensureAllowed(limiterKey);

        TwoFactorSettings settings = twoFactor.findByUserIdForUpdate(userId)
                .filter(TwoFactorSettings::isEnabled)
                .orElseThrow(TwoFactorLoginService::invalidChallenge);
        User user = users.findById(userId).orElseThrow(TwoFactorLoginService::invalidChallenge);

        if (!checker.check(settings, code)) {
            limiter.recordFailure(limiterKey);
            throw new UnauthenticatedException("auth.invalid_2fa_code", "El codigo de verificacion no es valido");
        }
        limiter.reset(limiterKey);
        twoFactor.save(settings);
        return sessions.openSession(user);
    }

    private static UnauthenticatedException invalidChallenge() {
        return new UnauthenticatedException("auth.challenge_invalid", "La verificacion expiro. Inicia sesion de nuevo");
    }
}