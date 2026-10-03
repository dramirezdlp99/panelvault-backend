package com.panelvault.backend.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.panelvault.backend.analysis.domain.AnalysisJob;
import com.panelvault.backend.analysis.domain.JobStatus;
import com.panelvault.backend.analysis.domain.TestImages;
import com.panelvault.backend.identity.domain.UserId;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisProcessorTest {

    private final AnalysisFixture f = new AnalysisFixture();
    private final UserId ana = UserId.newId();

    private AnalysisJob encolar(int seed) {
        return ((SubmissionResult.Queued) f.submit.submit(ana, TestImages.pngVariant(seed), "western")).job();
    }

    private AnalysisJob estado(AnalysisJob job) {
        return f.jobs.findById(job.id()).orElseThrow();
    }

    @Test
    void procesaElTrabajoGuardaElResultadoYBorraLaImagen() {
        AnalysisJob job = encolar(1);

        assertThat(f.processor.runOnce()).isEqualTo(1);

        assertThat(estado(job).status()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(f.results.find(job.pageHash(), job.preset())).isPresent();
        assertThat(f.images.contains(job.id())).isFalse();
    }

    @Test
    void conLaColaVaciaNoHaceNada() {
        assertThat(f.processor.runOnce()).isZero();
        assertThat(f.analyzer.calls()).isZero();
    }

    @Test
    void unFalloTemporalDevuelveElTrabajoALaColaConEspera() {
        AnalysisJob job = encolar(1);
        f.analyzer.failNext("El motor de IA tardo demasiado", true);

        f.processor.runOnce();

        AnalysisJob despues = estado(job);
        assertThat(despues.status()).isEqualTo(JobStatus.PENDING);
        assertThat(despues.lastError()).contains("tardo demasiado");
        // Politica sin azar: tras el intento 1 espera la mitad de 10 s.
        assertThat(despues.availableAt()).isEqualTo(AnalysisFixture.START.plusSeconds(5));
        assertThat(f.images.contains(job.id())).isTrue();

        // Antes de la espera no se reintenta; despues si, y esta vez sale bien.
        assertThat(f.processor.runOnce()).isZero();
        f.clock.advance(Duration.ofSeconds(5));
        assertThat(f.processor.runOnce()).isEqualTo(1);
        assertThat(estado(job).status()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(estado(job).attempts()).isEqualTo(2);
    }

    @Test
    void unFalloDefinitivoTerminaElTrabajoSinReintentar() {
        AnalysisJob job = encolar(1);
        f.analyzer.failNext("El motor de IA respondio 422: imagen invalida", false);

        f.processor.runOnce();

        assertThat(estado(job).status()).isEqualTo(JobStatus.FAILED);
        assertThat(f.images.contains(job.id())).isFalse();
    }

    @Test
    void alAgotarLosReintentosElTrabajoFalla() {
        AnalysisJob job = encolar(1);
        for (int i = 0; i < 3; i++) {
            f.analyzer.failNext("caido", true);
            f.processor.runOnce();
            f.clock.advance(Duration.ofMinutes(10));
        }

        assertThat(estado(job).status()).isEqualTo(JobStatus.FAILED);
        assertThat(estado(job).attempts()).isEqualTo(3);
    }

    @Test
    void siElMotorNoDespiertaElLoteEnteroVuelveALaCola() {
        AnalysisJob a = encolar(1);
        AnalysisJob b = encolar(2);
        f.analyzer.beUnavailable("No se pudo conectar con el motor de IA");

        assertThat(f.processor.runOnce()).isEqualTo(2);

        assertThat(List.of(estado(a).status(), estado(b).status())).containsOnly(JobStatus.PENDING);
        assertThat(f.analyzer.calls()).isZero();
    }

    @Test
    void unWorkerLentoNoPisaElTrabajoQueOtroRetomo() {
        AnalysisJob job = encolar(1);
        ClaimedJob lento = f.lifecycle.claimBatch().getFirst();

        // El lease del worker lento vence y otro worker retoma el trabajo.
        f.clock.advance(AnalysisFixture.LEASE.plusSeconds(1));
        ClaimedJob rapido = f.lifecycle.claimBatch().getFirst();

        f.lifecycle.recordFailure(lento, "respuesta tardia", true);
        assertThat(estado(job).status()).isEqualTo(JobStatus.RUNNING);

        f.lifecycle.recordSuccess(rapido, FakePanelAnalyzer.RESPONSE);
        assertThat(estado(job).status()).isEqualTo(JobStatus.SUCCEEDED);
    }

    @Test
    void procesaComoMaximoUnLotePorVuelta() {
        // 3 paginas de Ana (su limite en la fixture) y 4 de otros usuarios: 7 en cola.
        for (int i = 0; i < 3; i++) {
            encolar(i);
        }
        for (int i = 10; i < 14; i++) {
            f.submit.submit(UserId.newId(), TestImages.pngVariant(i), "western");
        }
        assertThat(f.jobs.all()).hasSize(7);

        // El lote es de 5: primero 5 y luego los 2 restantes.
        assertThat(f.processor.runOnce()).isEqualTo(5);
        assertThat(f.processor.runOnce()).isEqualTo(2);
    }
}
