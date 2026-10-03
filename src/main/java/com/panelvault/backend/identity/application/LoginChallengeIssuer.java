package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.UserId;
import java.time.Instant;

/**
 * Puerto para emitir y verificar el ticket del segundo paso del login.
 *
 * <p>El ticket solo sirve para enviar el codigo 2FA: no da acceso a ninguna otra ruta de la API.
 */
public interface LoginChallengeIssuer {

    IssuedChallenge issue(UserId userId, Instant now);

    /**
     * Devuelve el usuario del ticket.
     *
     * @throws com.panelvault.backend.shared.error.UnauthenticatedException si es invalido o vencio
     */
    UserId verify(String token);
}