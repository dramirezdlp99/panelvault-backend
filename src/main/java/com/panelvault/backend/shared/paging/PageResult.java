package com.panelvault.backend.shared.paging;

import java.util.List;
import java.util.function.Function;

/**
 * Una pagina de resultados junto con lo necesario para dibujar la paginacion en el frontend.
 *
 * @param items      elementos de esta pagina
 * @param page       numero de pagina (desde 0)
 * @param size       tamano de pagina pedido
 * @param totalItems total de elementos en todas las paginas
 * @param totalPages total de paginas
 */
public record PageResult<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public PageResult {
        items = List.copyOf(items);
    }

    public static <T> PageResult<T> of(List<T> items, PageQuery query, long totalItems) {
        int totalPages = (int) ((totalItems + query.size() - 1) / query.size());
        return new PageResult<>(items, query.page(), query.size(), totalItems, totalPages);
    }

    /** Transforma los elementos conservando la paginacion (por ejemplo, de dominio a respuesta web). */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalItems, totalPages);
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }
}
