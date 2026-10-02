package com.panelvault.backend.identity.domain;

/**
 * Roles de PanelVault (RBAC). El permiso se decide SIEMPRE en el backend: el frontend solo
 * usa el rol para mostrar u ocultar opciones, nunca para autorizar.
 */
public enum Role {

    /** Lee y organiza su propia biblioteca. Es el rol de toda cuenta nueva. */
    LECTOR,

    /** Ademas de leer, publica y corrige obras del catalogo publico (dominio publico). */
    CURADOR,

    /** Ademas de curar, administra cuentas y asigna roles. */
    ADMIN
}