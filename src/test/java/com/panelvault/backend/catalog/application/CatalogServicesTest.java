package com.panelvault.backend.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.catalog.domain.CatalogWork;
import com.panelvault.backend.catalog.domain.License;
import com.panelvault.backend.catalog.domain.WorkDetails;
import com.panelvault.backend.identity.application.MutableClock;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.NotFoundException;
import com.panelvault.backend.shared.paging.PageQuery;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogServicesTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-03T15:00:00Z"));
    private final InMemoryCatalogRepository repository = new InMemoryCatalogRepository();
    private final CurationService curation = new CurationService(repository, new SlugAllocator(repository), clock);
    private final CatalogService catalog = new CatalogService(repository);
    private final UserId curador = UserId.newId();

    private static WorkDetails obra(String title, String author) {
        return new WorkDetails(title, author, 1913, null, null, "https://en.wikipedia.org/wiki/Krazy_Kat", null,
                License.PUBLIC_DOMAIN, null, List.of());
    }

    private CatalogWork publicada(String title, String author) {
        CatalogWork work = curation.create(curador, obra(title, author));
        return curation.publish(work.id());
    }

    @Test
    void crearAsignaUnSlugUnicoAunqueSeRepitaElTitulo() {
        assertThat(curation.create(curador, obra("Krazy Kat", "Herriman")).slug().value()).isEqualTo("krazy-kat");
        assertThat(curation.create(curador, obra("Krazy Kat", "Herriman")).slug().value()).isEqualTo("krazy-kat-2");
        assertThat(curation.create(curador, obra("Krazy  KAT!", "Herriman")).slug().value()).isEqualTo("krazy-kat-3");
    }

    @Test
    void editarNoCambiaElSlug() {
        CatalogWork work = curation.create(curador, obra("Krazy Kat", "Herriman"));

        CatalogWork editada = curation.update(work.id(), obra("Krazy Kat (1913)", "George Herriman"));

        assertThat(editada.slug().value()).isEqualTo("krazy-kat");
        assertThat(editada.details().author()).isEqualTo("George Herriman");
    }

    @Test
    void elPublicoSoloVeObrasPublicadas() {
        CatalogWork borrador = curation.create(curador, obra("Borrador", "Alguien"));
        publicada("Little Nemo", "Winsor McCay");

        assertThat(catalog.list(null, PageQuery.of(0, 10)).items()).extracting(w -> w.details().title())
                .containsExactly("Little Nemo");
        assertThat(catalog.publishedSlugs()).containsExactly("little-nemo");
        assertThatThrownBy(() -> catalog.get(borrador.slug().value())).isInstanceOf(NotFoundException.class);
        assertThat(curation.list(null, PageQuery.of(0, 10)).totalItems()).isEqualTo(2);
    }

    @Test
    void buscaPorTituloOAutor() {
        publicada("Little Nemo", "Winsor McCay");
        publicada("The Yellow Kid", "Richard F. Outcault");

        assertThat(catalog.list("mccay", PageQuery.of(0, 10)).items()).extracting(w -> w.details().title())
                .containsExactly("Little Nemo");
        assertThat(catalog.list("yellow", PageQuery.of(0, 10)).totalItems()).isEqualTo(1);
    }

    @Test
    void unSlugInvalidoORetiradoResponde404() {
        CatalogWork work = publicada("Krazy Kat", "Herriman");
        assertThat(catalog.get("krazy-kat").id()).isEqualTo(work.id());

        curation.unpublish(work.id());
        assertThatThrownBy(() -> catalog.get("krazy-kat")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> catalog.get("NO VALIDO!!")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void borrarQuitaLaObra() {
        CatalogWork work = publicada("Krazy Kat", "Herriman");

        curation.delete(work.id());

        assertThatThrownBy(() -> curation.get(work.id()))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("catalog.work_not_found");
    }
}
