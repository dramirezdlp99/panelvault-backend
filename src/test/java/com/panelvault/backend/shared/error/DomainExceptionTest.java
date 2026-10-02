package com.panelvault.backend.shared.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DomainExceptionTest {

    @Test
    void conservaCategoriaCodigoYMensaje() {
        NotFoundException ex = new NotFoundException("comic.not_found", "El comic no existe");

        assertThat(ex.category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(ex.code()).isEqualTo("comic.not_found");
        assertThat(ex.getMessage()).isEqualTo("El comic no existe");
    }

    @Test
    void rechazaUnCodigoVacio() {
        assertThatThrownBy(() -> new ConflictException(" ", "mensaje"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codigo");
    }
}