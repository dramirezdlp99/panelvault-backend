package com.panelvault.backend.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.InvalidInputException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogDomainTest {

    private static final Instant AHORA = Instant.parse("2026-10-03T15:00:00Z");

    static WorkDetails obra(String title, String sourceUrl) {
        return new WorkDetails(title, "Winsor McCay", 1905, "New York Herald", "Descripcion", sourceUrl, null,
                License.PUBLIC_DOMAIN, null, List.of("Clasicos"));
    }

    // ------------------------------------------------------------------
    // WorkSlug
    // ------------------------------------------------------------------
    @Test
    void elSlugQuitaTildesSignosYEspacios() {
        assertThat(WorkSlug.fromTitle("¡Little Nemo in Slumberland!").value()).isEqualTo("little-nemo-in-slumberland");
        assertThat(WorkSlug.fromTitle("  Ñandú   Azul: Año 1  ").value()).isEqualTo("nandu-azul-ano-1");
        assertThat(WorkSlug.fromTitle("Hogan's Alley").value()).isEqualTo("hogan-s-alley");
    }

    @Test
    void unTituloSinLetrasNiNumerosProduceUnSlugGenerico() {
        assertThat(WorkSlug.fromTitle("¿¡!?").value()).isEqualTo("obra");
        assertThat(WorkSlug.fromTitle(null).value()).isEqualTo("obra");
    }

    @Test
    void unTituloLargoSeCortaSinPartirPalabras() {
        String slug = WorkSlug.fromTitle("palabra ".repeat(30)).value();
        assertThat(slug.length()).isLessThanOrEqualTo(WorkSlug.MAX_LENGTH);
        assertThat(slug).endsWith("palabra").doesNotEndWith("-");
    }

    @Test
    void lasVariantesNumeradasRespetanElLargoMaximo() {
        assertThat(WorkSlug.fromTitle("Krazy Kat").withSuffix(2).value()).isEqualTo("krazy-kat-2");
        String largo = WorkSlug.fromTitle("x".repeat(80)).withSuffix(12).value();
        assertThat(largo).hasSize(WorkSlug.MAX_LENGTH).endsWith("-12");
    }

    @Test
    void rechazaSlugsMalFormados() {
        assertThatThrownBy(() -> new WorkSlug("Con Mayusculas")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new WorkSlug("-guion-inicial")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new WorkSlug("doble--guion")).isInstanceOf(InvalidInputException.class);
    }

    // ------------------------------------------------------------------
    // WorkDetails
    // ------------------------------------------------------------------
    @Test
    void soloSeAceptanUrlsHttpsConDominio() {
        assertThat(obra("Nemo", "https://en.wikipedia.org/wiki/Little_Nemo").sourceUrl())
                .isEqualTo("https://en.wikipedia.org/wiki/Little_Nemo");
        assertThatThrownBy(() -> obra("Nemo", "http://inseguro.com")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> obra("Nemo", "javascript:alert(1)")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> obra("Nemo", "https:///sin-dominio")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> obra("Nemo", "https://usuario:clave@sitio.com")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> obra("Nemo", " ")).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void validaElAnoYNormalizaEtiquetas() {
        assertThatThrownBy(() -> new WorkDetails("T", "A", 1500, null, null, "https://a.org", null,
                License.CC0, null, List.of())).isInstanceOf(InvalidInputException.class);
        WorkDetails d = new WorkDetails("T", "A", null, null, null, "https://a.org", null, License.CC_BY, 10,
                List.of(" Humor ", "humor", "Tira de prensa"));
        assertThat(d.tags()).containsExactly("humor", "tira de prensa");
    }

    // ------------------------------------------------------------------
    // CatalogWork
    // ------------------------------------------------------------------
    @Test
    void unaObraNaceComoBorradorYSePuedePublicarYDespublicar() {
        CatalogWork work = CatalogWork.draft(WorkSlug.fromTitle("Nemo"), obra("Nemo", "https://a.org"),
                UserId.newId(), AHORA);
        assertThat(work.published()).isFalse();

        work.publish(AHORA.plusSeconds(10));
        assertThat(work.published()).isTrue();
        assertThat(work.publishedAt()).isEqualTo(AHORA.plusSeconds(10));

        work.publish(AHORA.plusSeconds(99));
        assertThat(work.publishedAt()).isEqualTo(AHORA.plusSeconds(10));

        work.unpublish(AHORA.plusSeconds(20));
        assertThat(work.published()).isFalse();
    }
}
