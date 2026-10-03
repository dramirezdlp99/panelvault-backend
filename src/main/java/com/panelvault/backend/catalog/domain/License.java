package com.panelvault.backend.catalog.domain;

/**
 * Licencia de una obra del catalogo. PanelVault solo publica obras que se pueden compartir
 * legalmente: dominio publico o licencias Creative Commons abiertas.
 */
public enum License {
    PUBLIC_DOMAIN,
    CC0,
    CC_BY,
    CC_BY_SA
}
