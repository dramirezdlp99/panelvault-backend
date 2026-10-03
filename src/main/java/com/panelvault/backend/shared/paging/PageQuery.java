package com.panelvault.backend.shared.paging;

import com.panelvault.backend.shared.error.InvalidInputException;

/**
 * Pagina pedida de un listado (Value Object). La pagina empieza en 0.
 *
 * <p>El tamano tiene tope para que nadie pida un millon de filas de una vez y tumbe el servidor.
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new InvalidInputException("request.invalid_page", "La pagina no puede ser negativa");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidInputException(
                    "request.invalid_page_size", "El tamano de pagina debe estar entre 1 y " + MAX_SIZE);
        }
    }

    /** Valores por defecto cuando el cliente no los envia. */
    public static PageQuery of(Integer page, Integer size) {
        return new PageQuery(page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

    public long offset() {
        return (long) page * size;
    }
}
