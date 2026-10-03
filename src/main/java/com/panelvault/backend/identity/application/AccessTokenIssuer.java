package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.User;
import java.time.Instant;

/**
 * Puerto para emitir access tokens.
 *
 * <p>La aplicacion solo sabe que obtiene un token con vencimiento; que sea un JWT firmado con
 * HMAC-SHA256 es un detalle del adaptador de infraestructura.
 */
public interface AccessTokenIssuer {

    IssuedAccessToken issue(User user, Instant now);
}