package com.panelvault.backend.reading.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.reading.domain.ProgressUpdate;
import com.panelvault.backend.shared.error.InvalidInputException;
import com.panelvault.backend.shared.error.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReadingServiceTest {

    private final ReadingFixture f = new ReadingFixture();
    private final UserId ana = UserId.newId();

    private ProgressUpdate cambio(int page, Instant when, String device) {
        return new ProgressUpdate(page, 2, true, when, device);
    }

    @Test
    void laPrimeraSincronizacionCreaElProgreso() {
        ComicId comic = f.comicDe(ana, 40);

        SyncResult result = f.reading.sync(ana, comic, cambio(3, ReadingFixture.START, "portatil"));

        assertThat(result.applied()).isTrue();
        assertThat(result.progress().currentPage()).isEqualTo(3);
        assertThat(result.progress().currentPanel()).isEqualTo(2);
        assertThat(result.progress().guidedMode()).isTrue();
        assertThat(f.reading.get(ana, comic).totalPages()).isEqualTo(40);
    }

    @Test
    void unCambioViejoNoSeAplicaYSeDevuelveElEstadoVigente() {
        ComicId comic = f.comicDe(ana, 40);
        f.reading.sync(ana, comic, cambio(25, ReadingFixture.START, "portatil"));

        SyncResult result = f.reading.sync(ana, comic, cambio(4, ReadingFixture.START.minusSeconds(600), "celular"));

        assertThat(result.applied()).isFalse();
        assertThat(result.progress().currentPage()).isEqualTo(25);
    }

    @Test
    void noSePuedeIrMasAllaDeLaUltimaPagina() {
        ComicId comic = f.comicDe(ana, 10);

        assertThatThrownBy(() -> f.reading.sync(ana, comic, cambio(11, ReadingFixture.START, "x")))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("reading.invalid_page");
    }

    @Test
    void noSePuedeSincronizarUnComicAjeno() {
        ComicId comic = f.comicDe(ana, 10);

        assertThatThrownBy(() -> f.reading.sync(UserId.newId(), comic, cambio(1, ReadingFixture.START, "x")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void sinProgresoSeResponde404() {
        ComicId comic = f.comicDe(ana, 10);

        assertThatThrownBy(() -> f.reading.get(ana, comic))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("reading.no_progress");
    }

    @Test
    void losRecientesVanDelUltimoLeidoAlPrimero() {
        ComicId primero = f.comicDe(ana, 10);
        ComicId segundo = f.comicDe(ana, 10);
        f.reading.sync(ana, primero, cambio(2, f.clock.instant(), "x"));
        f.clock.advance(Duration.ofMinutes(5));
        f.reading.sync(ana, segundo, cambio(2, f.clock.instant(), "x"));

        assertThat(f.reading.recent(ana, 10)).extracting(r -> r.comic().id()).containsExactly(segundo, primero);
        assertThat(f.reading.recent(ana, 1)).hasSize(1);
    }

    @Test
    void elResumenDeLaBibliotecaCuentaEmpezadosYTerminados() {
        ComicId uno = f.comicDe(ana, 10);
        ComicId dos = f.comicDe(ana, 10);
        f.reading.sync(ana, uno, cambio(10, ReadingFixture.START, "x"));
        f.reading.sync(ana, dos, cambio(3, ReadingFixture.START, "x"));

        var stats = f.library.stats(ana);
        assertThat(stats.comicsStarted()).isEqualTo(2);
        assertThat(stats.comicsFinished()).isEqualTo(1);
    }
}
