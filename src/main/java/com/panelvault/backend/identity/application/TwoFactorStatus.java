package com.panelvault.backend.identity.application;

/** Estado de la 2FA de un usuario: si esta activa y cuantos codigos de recuperacion le quedan. */
public record TwoFactorStatus(boolean enabled, int recoveryCodesRemaining) {}