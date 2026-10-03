package com.panelvault.backend.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.analysis.domain.AnalysisJob;
import com.panelvault.backend.analysis.domain.AnalysisPreset;
import com.panelvault.backend.analysis.domain.JobStatus;
import com.panelvault.backend.analysis.domain.PageHash;
import com.panelvault.backend.analysis.domain.TestImages;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.InvalidInputException;
import com.panelvault.backend.shared.error.TooManyRequestsException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SubmitPageServiceTest {

    private final AnalysisFixture f = new AnalysisFixture();
    private final UserId ana = UserId.newId();
    private final UserId beto = UserId.newId();

    private AnalysisJob encolar(UserId user, byte[] image) {
        SubmissionResult result = f.submit.submit(user, image, "western");
        assertThat(result).isInstanceOf(SubmissionResult.Queued.class);
        return ((SubmissionResult.Queued) result).job();
    }

    @Test
    void unaPaginaNuevaQuedaEnColaConSuImagenGuardada() {
        AnalysisJob job = encolar(ana, TestImages.PNG);

        assertThat(job.status()).isEqualTo(JobStatus.PENDING);
        assertThat(job.pageHash()).isEqualTo(PageHash.of(TestImages.PNG));
        assertThat(job.preset()).isEqualTo(AnalysisPreset.WESTERN);
        assertThat(f.images.contains(job.id())).isTrue();
    }

    @Test
    void laMismaPaginaEnColaNoSeDuplicaAunqueLaSubaOtroUsuario() {
        AnalysisJob primero = encolar(ana, TestImages.PNG);
        AnalysisJob segundo = encolar(beto, TestImages.PNG);

        assertThat(segundo.id()).isEqualTo(primero.id());
        assertThat(f.jobs.all()).hasSize(1);
    }

    @Test
    void laMismaPaginaEnOtroModoDeLecturaEsOtroTrabajo() {
        AnalysisJob occidental = encolar(ana, TestImages.PNG);
        SubmissionResult manga = f.submit.submit(ana, TestImages.PNG, "manga");

        assertThat(((SubmissionResult.Queued) manga).job().id()).isNotEqualTo(occidental.id());
    }

    @Test
    void unaPaginaYaAnalizadaSeRespondeDesdeLaCacheSinEncolar() {
        encolar(ana, TestImages.PNG);
        f.processor.runOnce();
        int llamadas = f.analyzer.calls();

        SubmissionResult result = f.submit.submit(beto, TestImages.PNG, "western");

        assertThat(result).isInstanceOf(SubmissionResult.Ready.class);
        assertThat(((SubmissionResult.Ready) result).result().payloadJson()).isEqualTo(FakePanelAnalyzer.RESPONSE);
        assertThat(f.analyzer.calls()).isEqualTo(llamadas);
    }

    @Test
    void rechazaArchivosQueNoSonImagenes() {
        byte[] html = "<html>no soy una imagen</html>".getBytes(StandardCharsets.US_ASCII);
        assertThatThrownBy(() -> f.submit.submit(ana, html, "western"))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("analysis.unsupported_image");
    }

    @Test
    void rechazaImagenesVaciasODemasiadoGrandes() {
        assertThatThrownBy(() -> f.submit.submit(ana, new byte[0], "western"))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("analysis.empty_image");
        byte[] enorme = java.util.Arrays.copyOf(TestImages.PNG, 2 * 1024 * 1024);
        assertThatThrownBy(() -> f.submit.submit(ana, enorme, "western"))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("analysis.image_too_large");
    }

    @Test
    void limitaLosTrabajosPendientesPorUsuario() {
        for (int i = 0; i < 3; i++) {
            encolar(ana, TestImages.pngVariant(i));
        }

        assertThatThrownBy(() -> f.submit.submit(ana, TestImages.pngVariant(99), "western"))
                .isInstanceOf(TooManyRequestsException.class)
                .extracting("code")
                .isEqualTo("analysis.too_many_pending");
        // El limite es por usuario: otro usuario si puede encolar.
        encolar(beto, TestImages.pngVariant(99));
    }
}
