package com.panelvault.backend.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.analysis.domain.AnalysisJob;
import com.panelvault.backend.analysis.domain.JobStatus;
import com.panelvault.backend.analysis.domain.TestImages;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.NotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnalysisQueryServiceTest {

    private final AnalysisFixture f = new AnalysisFixture();
    private final UserId ana = UserId.newId();

    private AnalysisJob encolar() {
        return ((SubmissionResult.Queued) f.submit.submit(ana, TestImages.PNG, "western")).job();
    }

    @Test
    void elDuenoVeSuTrabajoPendienteSinResultado() {
        AnalysisJob job = encolar();

        JobView view = f.queries.job(job.id(), ana);

        assertThat(view.job().status()).isEqualTo(JobStatus.PENDING);
        assertThat(view.result()).isEmpty();
    }

    @Test
    void alTerminarElTrabajoIncluyeElResultado() {
        AnalysisJob job = encolar();
        f.processor.runOnce();

        assertThat(f.queries.job(job.id(), ana).result()).isPresent();
    }

    @Test
    void otroUsuarioRecibe404ComoSiNoExistiera() {
        AnalysisJob job = encolar();

        assertThatThrownBy(() -> f.queries.job(job.id(), UserId.newId()))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("analysis.job_not_found");
        assertThatThrownBy(() -> f.queries.job(UUID.randomUUID(), ana)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void elResultadoSeBuscaPorHuellaYModo() {
        AnalysisJob job = encolar();
        f.processor.runOnce();

        assertThat(f.queries.result(job.pageHash().value(), "western").payloadJson())
                .isEqualTo(FakePanelAnalyzer.RESPONSE);
        assertThatThrownBy(() -> f.queries.result(job.pageHash().value(), "manga"))
                .isInstanceOf(NotFoundException.class)
                .extracting("code")
                .isEqualTo("analysis.result_not_found");
    }
}
