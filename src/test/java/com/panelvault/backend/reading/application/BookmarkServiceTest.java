package com.panelvault.backend.reading.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.reading.domain.Bookmark;
import com.panelvault.backend.shared.error.InvalidInputException;
import com.panelvault.backend.shared.error.NotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookmarkServiceTest {

    private final ReadingFixture f = new ReadingFixture();
    private final UserId ana = UserId.newId();

    @Test
    void guardarConElMismoIdEsIdempotenteYPermiteEditar() {
        ComicId comic = f.comicDe(ana, 30);
        UUID id = UUID.randomUUID();

        assertThat(f.bookmarkService.save(ana, comic, id, 5, "  Primera aparicion  ").created()).isTrue();
        BookmarkSaveResult editado = f.bookmarkService.save(ana, comic, id, 6, "Mejor esta");

        assertThat(editado.created()).isFalse();
        assertThat(f.bookmarkService.list(ana, comic))
                .singleElement()
                .satisfies(b -> {
                    assertThat(b.page()).isEqualTo(6);
                    assertThat(b.note()).isEqualTo("Mejor esta");
                });
    }

    @Test
    void seListanOrdenadosPorPagina() {
        ComicId comic = f.comicDe(ana, 30);
        f.bookmarkService.save(ana, comic, UUID.randomUUID(), 20, "");
        f.bookmarkService.save(ana, comic, UUID.randomUUID(), 3, "");
        f.bookmarkService.save(ana, comic, UUID.randomUUID(), 11, "");

        assertThat(f.bookmarkService.list(ana, comic)).extracting(Bookmark::page).containsExactly(3, 11, 20);
    }

    @Test
    void laPaginaDebeExistirEnElComic() {
        ComicId comic = f.comicDe(ana, 10);

        assertThatThrownBy(() -> f.bookmarkService.save(ana, comic, UUID.randomUUID(), 11, ""))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void otroUsuarioNoPuedeVerEditarNiBorrarLosMarcadores() {
        ComicId comic = f.comicDe(ana, 10);
        UUID id = UUID.randomUUID();
        f.bookmarkService.save(ana, comic, id, 2, "");
        UserId beto = UserId.newId();

        assertThatThrownBy(() -> f.bookmarkService.list(beto, comic)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> f.bookmarkService.delete(beto, id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void borrarQuitaElMarcador() {
        ComicId comic = f.comicDe(ana, 10);
        UUID id = UUID.randomUUID();
        f.bookmarkService.save(ana, comic, id, 2, "");

        f.bookmarkService.delete(ana, id);

        assertThat(f.bookmarkService.list(ana, comic)).isEmpty();
        assertThatThrownBy(() -> f.bookmarkService.delete(ana, id))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("reading.bookmark_not_found");
    }
}
