package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.TwoFactorSettings;
import com.panelvault.backend.shared.crypto.Totp;
import java.time.Clock;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

/**
 * Verifica el codigo del segundo paso: un codigo TOTP de 6 digitos o un codigo de recuperacion.
 *
 * <p>Si acierta, modifica {@link TwoFactorSettings} (marca el intervalo como usado o consume el
 * codigo de recuperacion); quien lo llama debe guardar la entidad.
 */
@Component
public class TwoFactorCodeChecker {

    /** Intervalos aceptados antes y despues del actual: tolera +-30 s de desfase de reloj. */
    static final int TIME_STEP_WINDOW = 1;

    private final SecretProtector protector;
    private final Clock clock;

    public TwoFactorCodeChecker(SecretProtector protector, Clock clock) {
        this.protector = protector;
        this.clock = clock;
    }

    /** Acepta un codigo TOTP o de recuperacion. */
    public boolean check(TwoFactorSettings settings, String code) {
        if (RecoveryCodes.looksLikeRecoveryCode(code)) {
            return settings.useRecoveryCode(protector.fingerprint(RecoveryCodes.normalize(code)));
        }
        OptionalLong step = matchingTimeStep(settings, code);
        return step.isPresent() && settings.acceptTimeStep(step.getAsLong());
    }

    /** Solo verifica un codigo TOTP, sin consumir nada. Lo usa la confirmacion de la activacion. */
    public OptionalLong matchingTimeStep(TwoFactorSettings settings, String code) {
        String digits = code == null ? "" : code.replaceAll("\\s", "");
        byte[] secret = protector.decrypt(settings.encryptedSecret(), settings.userId().toString());
        return Totp.verify(secret, digits, clock.instant(), TIME_STEP_WINDOW);
    }
}