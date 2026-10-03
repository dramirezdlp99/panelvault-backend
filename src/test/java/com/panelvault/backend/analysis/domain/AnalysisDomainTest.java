package com.panelvault.backend.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.shared.error.InvalidInputException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Pruebas de los tipos de valor del modulo: huella, modo de lectura y formato de imagen. */
class AnalysisDomainTest {

    // ------------------------------------------------------------------
    // PageHash
    // ------------------------------------------------------------------
    @Test
    void laHuellaEsElSha256EstandarDeLosBytes() {
        // Vector de prueba de FIPS 180-2 para "abc".
        assertThat(PageHash.of("abc".getBytes(StandardCharsets.US_ASCII)).value())
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void laHuellaSeNormalizaAMinusculas() {
        String mayusculas = "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD";
        assertThat(new PageHash(mayusculas)).isEqualTo(PageHash.of("abc".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void rechazaHuellasMalFormadas() {
        assertThatThrownBy(() -> new PageHash("abc")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new PageHash("z".repeat(64))).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new PageHash(null)).isInstanceOf(InvalidInputException.class);
    }

    // ------------------------------------------------------------------
    // AnalysisPreset
    // ------------------------------------------------------------------
    @Test
    void reconoceLosModosDeLecturaSinImportarMayusculas() {
        assertThat(AnalysisPreset.fromValue("Manga")).isEqualTo(AnalysisPreset.MANGA);
        assertThat(AnalysisPreset.fromValue(" western ")).isEqualTo(AnalysisPreset.WESTERN);
    }

    @Test
    void sinModoSeAsumeLecturaOccidental() {
        assertThat(AnalysisPreset.fromValue(null)).isEqualTo(AnalysisPreset.WESTERN);
        assertThat(AnalysisPreset.fromValue("")).isEqualTo(AnalysisPreset.WESTERN);
    }

    @Test
    void rechazaModosDesconocidos() {
        assertThatThrownBy(() -> AnalysisPreset.fromValue("vertical"))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("analysis.unknown_preset");
    }

    // ------------------------------------------------------------------
    // ImageFormat
    // ------------------------------------------------------------------
    @Test
    void detectaJpegPngYWebpPorSuFirmaBinaria() {
        assertThat(ImageFormat.detect(TestImages.JPEG)).contains(ImageFormat.JPEG);
        assertThat(ImageFormat.detect(TestImages.PNG)).contains(ImageFormat.PNG);
        assertThat(ImageFormat.detect(TestImages.WEBP)).contains(ImageFormat.WEBP);
    }

    @Test
    void noSeDejaEnganarPorContenidoQueNoEsImagen() {
        assertThat(ImageFormat.detect("<html>hola</html>".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(ImageFormat.detect(new byte[] {(byte) 0xFF})).isEmpty();
        assertThat(ImageFormat.detect(new byte[0])).isEmpty();
        assertThat(ImageFormat.detect(null)).isEmpty();
        // RIFF pero no WEBP (por ejemplo, un WAV de audio).
        assertThat(ImageFormat.detect("RIFF0000WAVEfmt ".getBytes(StandardCharsets.US_ASCII))).isEmpty();
    }
}
