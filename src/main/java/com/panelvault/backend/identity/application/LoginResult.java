package com.panelvault.backend.identity.application;

/**
 * Resultado del primer paso del login. Es una interfaz sellada: solo hay dos casos posibles, y el
 * compilador obliga a tratar ambos.
 */
public sealed interface LoginResult {

    /** Sin 2FA activa: la sesion queda abierta y estos son sus tokens. */
    record Authenticated(AuthTokens tokens) implements LoginResult {}

    /** Con 2FA activa: la contrasena es correcta pero falta el codigo de la app. */
    record TwoFactorRequired(IssuedChallenge challenge) implements LoginResult {}
}