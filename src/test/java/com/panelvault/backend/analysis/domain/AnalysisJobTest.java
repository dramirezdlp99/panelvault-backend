package com.panelvault.backend.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnalysisJobTest {

    private static final Instant AHORA = Instant.parse("2026-10-03T15:00:00Z");
    private static final Duration LEASE = Duration.ofMinutes(3);

    private AnalysisJob nuevo() {
        return AnalysisJob.enqueue(PageHash.of(TestImages.PNG), AnalysisPreset.WESTERN, UserId.newId(), AHORA);
    }

    @Test
    void unTrabajoNuevoQuedaPendienteYDisponibleDeInmediato() {
        AnalysisJob job = nuevo();

        assertThat(job.status()).isEqualTo(JobStatus.PENDING);
        assertThat(job.attempts()).isZero();
        assertThat(job.isClaimable(AHORA)).isTrue();
    }

    @Test
    void tomarloLoPoneEnCursoCuentaUnIntentoYArrienda() {
        AnalysisJob job = nuevo();
        job.claim(AHORA, LEASE);

        assertThat(job.status()).isEqualTo(JobStatus.RUNNING);
        assertThat(job.attempts()).isEqualTo(1);
        assertThat(job.leaseUntil()).isEqualTo(AHORA.plus(LEASE));
        assertThat(job.isClaimable(AHORA.plusSeconds(60))).isFalse();
    }

    @Test
    void siElLeaseVenceOtroWorkerPuedeRetomarlo() {
        AnalysisJob job = nuevo();
        job.claim(AHORA, LEASE);

        Instant despues = AHORA.plus(LEASE).plusSeconds(1);
        assertThat(job.isClaimable(despues)).isTrue();
        job.claim(despues, LEASE);
        assertThat(job.attempts()).isEqualTo(2);
    }

    @Test
    void elTokenDeCercaDistingueAlDuenoActualDelAnterior() {
        AnalysisJob job = nuevo();
        job.claim(AHORA, LEASE);
        int primerWorker = job.attempts();
        job.claim(AHORA.plus(LEASE).plusSeconds(1), LEASE);

        assertThat(job.holdsLease(primerWorker)).isFalse();
        assertThat(job.holdsLease(job.attempts())).isTrue();
    }

    @Test
    void terminarConExitoLimpiaElLeaseYElError() {
        AnalysisJob job = nuevo();
        job.claim(AHORA, LEASE);
        job.succeed(AHORA.plusSeconds(5));

        assertThat(job.status()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(job.leaseUntil()).isNull();
        assertThat(job.status().isFinished()).isTrue();
    }

    @Test
    void unFalloConReintentoVuelveALaColaParaMasTarde() {
        AnalysisJob job = nuevo();
        job.claim(AHORA, LEASE);
        job.fail("motor dormido", Optional.of(Duration.ofSeconds(20)), AHORA.plusSeconds(5));

        assertThat(job.status()).isEqualTo(JobStatus.PENDING);
        assertThat(job.lastError()).isEqualTo("motor dormido");
        assertThat(job.isClaimable(AHORA.plusSeconds(10))).isFalse();
        assertThat(job.isClaimable(AHORA.plusSeconds(25))).isTrue();
    }

    @Test
    void unFalloSinReintentoTerminaElTrabajo() {
        AnalysisJob job = nuevo();
        job.claim(AHORA, LEASE);
        job.fail("imagen corrupta", Optional.empty(), AHORA);

        assertThat(job.status()).isEqualTo(JobStatus.FAILED);
        assertThat(job.isClaimable(AHORA.plusSeconds(3600))).isFalse();
    }

    @Test
    void elErrorGuardadoSeRecortaAlLargoDeLaColumna() {
        AnalysisJob job = nuevo();
        job.claim(AHORA, LEASE);
        job.fail("x".repeat(2000), Optional.empty(), AHORA);

        assertThat(job.lastError()).hasSize(AnalysisJob.MAX_ERROR_LENGTH);
    }

    @Test
    void noSePuedeTerminarUnTrabajoQueNoEstaEnCurso() {
        assertThatThrownBy(() -> nuevo().succeed(AHORA)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> nuevo().fail("x", Optional.empty(), AHORA)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void noSePuedeTomarUnTrabajoNoDisponible() {
        AnalysisJob job = nuevo();
        job.claim(AHORA, LEASE);
        assertThatThrownBy(() -> job.claim(AHORA, LEASE)).isInstanceOf(IllegalStateException.class);
    }
}
