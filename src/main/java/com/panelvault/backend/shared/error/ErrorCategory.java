package com.panelvault.backend.shared.error;

/**
 * Categoria de un error de negocio.
 *
 * <p>Es deliberadamente independiente de HTTP: el dominio dice QUE paso (no existe, ya existe,
 * viola una regla) y la capa web decide como traducirlo a un codigo de estado.
 */
public enum ErrorCategory {
    INVALID_INPUT,
    NOT_FOUND,
    CONFLICT,
    BUSINESS_RULE,
    UNAUTHENTICATED,
    FORBIDDEN
}