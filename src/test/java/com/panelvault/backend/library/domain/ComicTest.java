package com.panelvault.backend.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.InvalidInputException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ComicTest {

    private static final Instant AHORA = Instant.parse("2026-10-03T15:00:00Z");

    private static ComicDetails detalles(String title, String series, int pages, List<String> tags) {
        return new ComicDetails(title, series, null, pages, ComicFormat.CBZ,
                new FileFingerprint(ComicSamples.sha256(1)), null, tags);
    }

    @Test
    void normalizaTextosYEtiquetas() {
        ComicDetails d = detalles("  Spider-Man   #1 ", "   ", 20, List.of(" Marvel ", "marvel", "SUPER  Heroes", " "));

        assertThat(d.title()).isEqualTo("Spider-Man #1");
        assertThat(d.series()).isNull();
        assertThat(d.tags()).containsExactly("marvel", "super heroes");
        assertThat(d.direction()).isEqualTo(ReadingDirection.LEFT_TO_RIGHT);
    }

    @Test
    void validaTituloPaginasYEtiquetas() {
        assertThatThrownBy(() -> detalles(" ", null, 10, null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> detalles("x".repeat(201), null, 10, null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> detalles("Titulo", null, 0, null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> detalles("Titulo", null, 2001, null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> detalles("Titulo", null, 10, List.of("con,coma")))
                .isInstanceOf(InvalidInputException.class);
        String[] once = new String[11];
        Arrays.setAll(once, i -> "tag" + i);
        assertThatThrownBy(() -> detalles("Titulo", null, 10, List.of(once)))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("library.too_many_tags");
    }

    @Test
    void laHuellaDebeSerUnSha256() {
        assertThatThrownBy(() -> new FileFingerprint("abc")).isInstanceOf(InvalidInputException.class);
        assertThat(new FileFingerprint(ComicSamples.sha256(1).toUpperCase()).value()).isEqualTo(ComicSamples.sha256(1));
    }

    @Test
    void unComicNuevoEmpiezaEnLaVersion1() {
        Comic comic = Comic.add(new ComicId(UUID.randomUUID()), UserId.newId(), ComicSamples.details(1), AHORA);
        assertThat(comic.version()).isEqualTo(1);
        assertThat(comic.createdAt()).isEqualTo(AHORA);
    }

    @Test
    void cambiarLosDatosSubeLaVersionPeroRepetirlosNo() {
        Comic comic = Comic.add(new ComicId(UUID.randomUUID()), UserId.newId(), ComicSamples.details(1), AHORA);

        assertThat(comic.update(ComicSamples.details(1), AHORA.plusSeconds(10))).isFalse();
        assertThat(comic.version()).isEqualTo(1);

        assertThat(comic.update(ComicSamples.details("Otro titulo", 24, 1), AHORA.plusSeconds(20))).isTrue();
        assertThat(comic.version()).isEqualTo(2);
        assertThat(comic.updatedAt()).isEqualTo(AHORA.plusSeconds(20));
    }

    @Test
    void elIdDebeSerUnUuid() {
        assertThatThrownBy(() -> ComicId.parse("123"))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("library.invalid_comic_id");
    }
}
