package com.panelvault.backend.identity.web;

import com.panelvault.backend.identity.application.TwoFactorSetupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestion de la 2FA del usuario autenticado ({@code /api/v1/me/2fa}).
 *
 * <ol>
 *   <li>{@code POST /setup}: genera el secreto y devuelve la URI para el QR.</li>
 *   <li>{@code POST /confirm}: con el primer codigo correcto activa la 2FA y devuelve los codigos
 *       de recuperacion.</li>
 *   <li>{@code GET}: estado actual. {@code POST /disable}: desactiva (exige un codigo).</li>
 * </ol>
 * Como en {@code /me}, el usuario sale del token: nadie puede tocar la 2FA de otra persona.
 */
@RestController
@RequestMapping("/api/v1/me/2fa")
public class TwoFactorController {

    private final TwoFactorSetupService setup;

    public TwoFactorController(TwoFactorSetupService setup) {
        this.setup = setup;
    }

    @GetMapping
    public TwoFactorStatusResponse status(@AuthenticationPrincipal Jwt jwt) {
        return TwoFactorStatusResponse.from(setup.status(CurrentUser.idOf(jwt)));
    }

    @PostMapping("/setup")
    public TwoFactorSetupResponse begin(@AuthenticationPrincipal Jwt jwt) {
        return TwoFactorSetupResponse.from(setup.begin(CurrentUser.idOf(jwt)));
    }

    @PostMapping("/confirm")
    public RecoveryCodesResponse confirm(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TwoFactorCodeRequest request) {
        return new RecoveryCodesResponse(setup.confirm(CurrentUser.idOf(jwt), request.code()));
    }

    @PostMapping("/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TwoFactorCodeRequest request) {
        setup.disable(CurrentUser.idOf(jwt), request.code());
    }
}