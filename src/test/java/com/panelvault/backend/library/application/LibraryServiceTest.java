package com.panelvault.backend.library.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.application.MutableClock;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.Comic;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.library.domain.ComicSamples;
import com.panelvault.backend.library.domain.LibraryStats;
import com.panelvault.backend.shared.error.ConflictException;
import com.panelvault.backend.shared.error.NotFoundException;
import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LibraryServiceTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-03T15:00:00Z"));
    private final InMemoryComicRepository comics = new InMemoryComicRepository();
    private final ReadingStatsProvider stats = new ReadingStatsProvider() {
        @Override
        public long comicsStarted(UserId user) {
            return 2;
        }

        @Override
        public long comicsFinished(UserId user) {
            return 1;
        }
    };
    private final LibraryService library = new LibraryService(comics, stats, clock);
    private final UserId ana = UserId.newId();
    private final UserId beto = UserId.newId();

    private static ComicId nuevoId() {
        return new ComicId(UUID.randomUUID());
    }

    @Test
    void guardarUnComicNuevoLoCreaYRepetirLaPeticionNoLoDuplica() {
        ComicId id = nuevoId();

        UpsertResult primera = library.save(ana, id, ComicSamples.details(1));
        UpsertResult repetida = library.save(ana, id, ComicSamples.details(1));

        assertThat(primera.created()).isTrue();
        assertThat(repetida.created()).isFalse();
        assertThat(repetida.comic().version()).isEqualTo(1);
        assertThat(comics.countByOwner(ana)).isEqualTo(1);
    }

    @Test
    void actualizarCambiaLosDatosYSubeLaVersion() {
        ComicId id = nuevoId();
        library.save(ana, id, ComicSamples.details(1));
        clock.advance(Duration.ofMinutes(1));

        Comic actualizado = library.save(ana, id, ComicSamples.details("Nuevo titulo", 24, 1)).comic();

        assertThat(actualizado.details().title()).isEqualTo("Nuevo titulo");
        assertThat(actualizado.version()).isEqualTo(2);
    }

    @Test
    void elMismoArchivoNoPuedeEstarDosVecesEnLaBiblioteca() {
        library.save(ana, nuevoId(), ComicSamples.details("Original", 24, 7));

        assertThatThrownBy(() -> library.save(ana, nuevoId(), ComicSamples.details("Copia", 24, 7)))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo("library.duplicate_file");
        // Otro usuario si puede tener el mismo archivo.
        assertThat(library.save(beto, nuevoId(), ComicSamples.details("Original", 24, 7)).created()).isTrue();
    }

    @Test
    void elComicDeOtroUsuarioSeRespondeComoInexistente() {
        ComicId id = nuevoId();
        library.save(ana, id, ComicSamples.details(1));

        assertThatThrownBy(() -> library.get(beto, id)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> library.save(beto, id, ComicSamples.details(2))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> library.delete(beto, id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listaDelMasRecienteAlMasAntiguoConBusquedaYPaginacion() {
        for (int i = 1; i <= 5; i++) {
            library.save(ana, nuevoId(), ComicSamples.details(i == 3 ? "Spider-Man" : "Comic " + i, 24, i));
            clock.advance(Duration.ofSeconds(1));
        }

        PageResult<Comic> pagina = library.list(ana, null, PageQuery.of(0, 2));
        assertThat(pagina.items()).extracting(c -> c.details().title()).containsExactly("Comic 5", "Comic 4");
        assertThat(pagina.totalItems()).isEqualTo(5);
        assertThat(pagina.totalPages()).isEqualTo(3);

        assertThat(library.list(ana, "spider", PageQuery.of(0, 10)).items())
                .extracting(c -> c.details().title())
                .containsExactly("Spider-Man");
        assertThat(library.list(beto, null, PageQuery.of(0, 10)).items()).isEmpty();
    }

    @Test
    void borrarQuitaElComic() {
        ComicId id = nuevoId();
        library.save(ana, id, ComicSamples.details(1));

        library.delete(ana, id);

        assertThatThrownBy(() -> library.get(ana, id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void elResumenCombinaBibliotecaYLectura() {
        library.save(ana, nuevoId(), ComicSamples.details("Uno", 20, 1));
        library.save(ana, nuevoId(), ComicSamples.details("Dos", 30, 2));

        assertThat(library.stats(ana)).isEqualTo(new LibraryStats(2, 50, 2, 1));
    }
}
