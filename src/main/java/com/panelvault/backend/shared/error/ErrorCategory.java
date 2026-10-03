package com.panelvault.backend.shared.error;

/**
 * Categoria de un error de negocio.
 *
 * <p>Es independiente de HTTP a proposito: el dominio dice QUE paso (no encontrado, conflicto,
 * demasiados intentos...) y solo la capa web decide con que codigo HTTP se responde. Asi el mismo
 * dominio serviria igual detras de otra interfaz.
 */
public enum ErrorCategory {

    /** Un dato no cumple las reglas (formato, politica de contrasena...). HTTP 400. */
    INVALID_INPUT,

    /** El recurso pedido no existe. HTTP 404. */
    NOT_FOUND,

    /** Choca con el estado actual (correo ya registrado, 2FA ya activo...). HTTP 409. */
    CONFLICT,

    /** Los datos son validos pero una regla de negocio lo impide. HTTP 422. */
    BUSINESS_RULE,

    /** No hay una identidad valida (credenciales, token, codigo). HTTP 401. */
    UNAUTHENTICATED,

    /** La identidad es valida pero no tiene permiso. HTTP 403. */
    FORBIDDEN,

    /** Se superaron los intentos permitidos; hay que esperar. HTTP 429. */
    RATE_LIMITED
}