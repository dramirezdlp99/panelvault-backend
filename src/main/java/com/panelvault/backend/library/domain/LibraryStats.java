package com.panelvault.backend.library.domain;

/**
 * Resumen de la biblioteca de un usuario, para el panel principal.
 *
 * @param comics         comics en la biblioteca
 * @param totalPages     paginas sumando todos los comics
 * @param comicsStarted  comics con progreso de lectura
 * @param comicsFinished comics leidos hasta la ultima pagina
 */
public record LibraryStats(long comics, long totalPages, long comicsStarted, long comicsFinished) {}
