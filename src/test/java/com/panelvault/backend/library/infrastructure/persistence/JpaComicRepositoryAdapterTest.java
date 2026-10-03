package com.panelvault.backend.library.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.DisplayName;
import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.identity.infrastructure.persistence.JpaUserRepositoryAdapter;
import com.panelvault.backend.library.domain.Comic;
import com.panelvault.backend.library.domain.ComicDetails;
import com.panelvault.backend.library.domain.ComicFormat;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.library.domain.ComicSamples;
import com.panelvault.backend.library.domain.FileFingerprint;
import com.panelvault.backend.library.domain.ReadingDirection;
import com.panelvault.backend.shared.paging.PageQuery;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Prueba de integracion de la biblioteca contra PostgreSQL (base panelvault_test). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaComicRepositoryAdapterTest {

    private static final Instant AHORA = Instant.parse("2026-10-03T15:00:00.123456Z");

    @Autowired
    private JpaComicRepositoryAdapter comics;

    @Autowired
    private JpaUserRepositoryAdapter users;

    private UserId ana;
    private int seed;

    @BeforeEach
    void setUp() {
        ana = users.save(User.register(new Email("biblio-" + UUID.randomUUID() + "@panelvault.test"),
                new DisplayName("Ana"), "$2a$12$hash", AHORA)).id();
    }

    private Comic guardar(String title, String series, Instant when) {
        seed++;
        ComicDetails details = new ComicDetails(title, series, "1", 24, ComicFormat.CBR,
                new FileFingerprint(ComicSamples.sha256(1000 + seed)), ReadingDirection.RIGHT_TO_LEFT,
                List.of("manga", "accion"));
        return comics.save(Comic.add(new ComicId(UUID.randomUUID()), ana, details, when));
    }

    @Test
    void guardaYLeeUnComicConTodosSusDatos() {
        Comic original = guardar("One Piece", "One Piece", AHORA);

        Comic leido = comics.findById(original.id()).orElseThrow();
        assertThat(leido.details()).isEqualTo(original.details());
        assertThat(leido.details().tags()).containsExactly("manga", "accion");
        assertThat(leido.owner()).isEqualTo(ana);
        assertThat(leido.version()).isEqualTo(1);
        assertThat(comics.findByOwnerAndFingerprint(ana, original.details().fingerprint())).isPresent();
    }

    @Test
    void listaPorFechaDeModificacionYPagina() {
        guardar("Primero", null, AHORA);
        guardar("Segundo", null, AHORA.plusSeconds(10));
        guardar("Tercero", null, AHORA.plusSeconds(20));

        var pagina = comics.findByOwner(ana, null, PageQuery.of(0, 2));
        assertThat(pagina.items()).extracting(c -> c.details().title()).containsExactly("Tercero", "Segundo");
        assertThat(pagina.totalItems()).isEqualTo(3);
        assertThat(comics.findByOwner(ana, "", PageQuery.of(1, 2)).items())
                .extracting(c -> c.details().title())
                .containsExactly("Primero");
    }

    @Test
    void buscaEnTituloYSerieSinDistinguirMayusculas() {
        guardar("Akira Vol. 1", "Akira", AHORA);
        guardar("Otra cosa", "Serie AKIRA", AHORA);
        guardar("Nada que ver", null, AHORA);

        assertThat(comics.findByOwner(ana, "akira", PageQuery.of(0, 10)).totalItems()).isEqualTo(2);
    }

    @Test
    void losComodinesDeSqlSeBuscanComoTextoNormal() {
        guardar("Descuento 100% real", null, AHORA);
        guardar("Descuento 1000 real", null, AHORA);
        guardar("nombre_con_guion", null, AHORA);
        guardar("nombreXconXguion", null, AHORA);

        assertThat(comics.findByOwner(ana, "100%", PageQuery.of(0, 10)).items())
                .extracting(c -> c.details().title())
                .containsExactly("Descuento 100% real");
        assertThat(comics.findByOwner(ana, "e_c", PageQuery.of(0, 10)).items())
                .extracting(c -> c.details().title())
                .containsExactly("nombre_con_guion");
    }

    @Test
    void cuentaComicsYPaginas() {
        guardar("Uno", null, AHORA);
        guardar("Dos", null, AHORA);

        assertThat(comics.countByOwner(ana)).isEqualTo(2);
        assertThat(comics.totalPagesByOwner(ana)).isEqualTo(48);
        assertThat(comics.totalPagesByOwner(UserId.newId())).isZero();
    }

    @Test
    void laBaseImpideElMismoArchivoDosVecesParaElMismoUsuario() {
        Comic original = guardar("Original", null, AHORA);
        Comic copia = Comic.add(new ComicId(UUID.randomUUID()), ana, original.details(), AHORA);

        assertThatThrownBy(() -> comics.save(copia)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elPatronLikeEscapaLosComodines() {
        assertThat(JpaComicRepositoryAdapter.likePattern(" 100%_x\\ ")).isEqualTo("%100\\%\\_x\\\\%");
    }
}
