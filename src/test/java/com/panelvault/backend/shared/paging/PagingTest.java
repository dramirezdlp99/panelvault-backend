package com.panelvault.backend.shared.paging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PagingTest {

    @Test
    void sinParametrosSeUsaLaPrimeraPaginaDe20() {
        PageQuery query = PageQuery.of(null, null);
        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
        assertThat(PageQuery.of(3, 10).offset()).isEqualTo(30);
    }

    @Test
    void rechazaPaginasNegativasYTamanosFueraDeRango() {
        assertThatThrownBy(() -> PageQuery.of(-1, 10)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> PageQuery.of(0, 0)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> PageQuery.of(0, 101)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void calculaElTotalDePaginasRedondeandoHaciaArriba() {
        PageQuery query = PageQuery.of(0, 10);
        assertThat(PageResult.of(List.of(), query, 0).totalPages()).isZero();
        assertThat(PageResult.of(List.of(), query, 10).totalPages()).isEqualTo(1);
        assertThat(PageResult.of(List.of(), query, 11).totalPages()).isEqualTo(2);
    }

    @Test
    void sabeSiHayUnaPaginaSiguiente() {
        assertThat(PageResult.of(List.of(1), PageQuery.of(0, 10), 25).hasNext()).isTrue();
        assertThat(PageResult.of(List.of(1), PageQuery.of(2, 10), 25).hasNext()).isFalse();
    }

    @Test
    void transformarLosElementosConservaLaPaginacion() {
        PageResult<String> result = PageResult.of(List.of(1, 2), PageQuery.of(1, 2), 6).map(i -> "#" + i);

        assertThat(result.items()).containsExactly("#1", "#2");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalItems()).isEqualTo(6);
        assertThat(result.totalPages()).isEqualTo(3);
    }
}
