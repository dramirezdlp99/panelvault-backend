package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.TwoFactorRepository;
import com.panelvault.backend.identity.domain.TwoFactorSettings;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.identity.domain.UserRepository;
import com.panelvault.backend.shared.crypto.Base32;
import com.panelvault.backend.shared.crypto.Totp;
import com.panelvault.backend.shared.error.BusinessRuleException;
import com.panelvault.backend.shared.error.ConflictException;
import com.panelvault.backend.shared.error.InvalidInputException;
import com.panelvault.backend.shared.error.NotFoundException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.OptionalLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso para que un usuario con sesion iniciada active, consulte o desactive su 2FA.
 *
 * <p>La activacion tiene dos pasos a proposito: primero se genera el secreto y se muestra el QR;
 * solo cuando el usuario envia un codigo correcto se activa. Asi nadie queda bloqueado por
 * activar la 2FA sin haber guardado el secreto en su app.
 */
@Service
public class TwoFactorSetupService {

    private final TwoFactorRepository twoFactor;
    private final UserRepository users;
    private final SecretProtector protector;
    private final TwoFactorCodeChecker checker;
    private final RecoveryCodes recoveryCodes;
    private final AttemptLimiter limiter;
    private final Clock clock;
    private final String issuerLabel;
    private final SecureRandom random = new SecureRandom();

    public TwoFactorSetupService(
            TwoFactorRepository twoFactor,
            UserRepository users,
            SecretProtector protector,
            TwoFactorCodeChecker checker,
            RecoveryCodes recoveryCodes,
            AttemptLimiter limiter,
            Clock clock,
            TwoFactorIssuerLabel issuerLabel) {
        this.twoFactor = twoFactor;
        this.users = users;
        this.protector = protector;
        this.checker = checker;
        this.recoveryCodes = recoveryCodes;
        this.limiter = limiter;
        this.clock = clock;
        this.issuerLabel = issuerLabel.value();
    }

    @Transactional(readOnly = true)
    public TwoFactorStatus status(UserId userId) {
        return twoFactor.findByUserId(userId)
                .filter(TwoFactorSettings::isEnabled)
                .map(s -> new TwoFactorStatus(true, s.remainingRecoveryCodes()))
                .orElse(new TwoFactorStatus(false, 0));
    }

    /** Paso 1: genera un secreto nuevo (reemplaza cualquier activacion pendiente anterior). */
    @Transactional
    public TwoFactorEnrollment begin(UserId userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("user.not_found", "No existe el usuario"));
        if (twoFactor.findByUserId(userId).map(TwoFactorSettings::isEnabled).orElse(false)) {
            throw new ConflictException("auth.2fa_already_enabled", "La verificacion en dos pasos ya esta activa");
        }
        byte[] secret = Totp.newSecret(random);
        twoFactor.save(TwoFactorSettings.pending(userId, protector.encrypt(secret, userId.toString()), clock.instant()));
        return new TwoFactorEnrollment(
                Base32.encode(secret), Totp.otpAuthUri(issuerLabel, user.email().value(), secret));
    }

    /** Paso 2: con un codigo correcto se activa y se entregan los codigos de recuperacion (una vez). */
    @Transactional
    public List<String> confirm(UserId userId, String code) {
        String limiterKey = "2fa:" + userId;
        limiter.ensureAllowed(limiterKey);
        TwoFactorSettings settings = twoFactor.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "auth.2fa_not_started", "Primero inicia la activacion de la verificacion en dos pasos"));
        if (settings.isEnabled()) {
            throw new ConflictException("auth.2fa_already_enabled", "La verificacion en dos pasos ya esta activa");
        }
        OptionalLong step = checker.matchingTimeStep(settings, code);
        if (step.isEmpty()) {
            limiter.recordFailure(limiterKey);
            throw invalidCode();
        }
        limiter.reset(limiterKey);

        List<String> codes = recoveryCodes.generate();
        List<String> fingerprints = codes.stream()
                .map(c -> protector.fingerprint(RecoveryCodes.normalize(c)))
                .toList();
        settings.enable(fingerprints, step.getAsLong(), clock.instant());
        twoFactor.save(settings);
        return codes;
    }

    /** Desactiva la 2FA. Exige un codigo valido: tener la sesion abierta no basta. */
    @Transactional
    public void disable(UserId userId, String code) {
        String limiterKey = "2fa:" + userId;
        limiter.ensureAllowed(limiterKey);
        TwoFactorSettings settings = twoFactor.findByUserIdForUpdate(userId)
                .filter(TwoFactorSettings::isEnabled)
                .orElseThrow(() -> new BusinessRuleException(
                        "auth.2fa_not_enabled", "La verificacion en dos pasos no esta activa"));
        if (!checker.check(settings, code)) {
            limiter.recordFailure(limiterKey);
            throw invalidCode();
        }
        limiter.reset(limiterKey);
        twoFactor.delete(userId);
    }

    private static InvalidInputException invalidCode() {
        return new InvalidInputException("auth.invalid_2fa_code", "El codigo de verificacion no es valido");
    }
}