package com.panelvault.backend.analysis.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.panelvault.backend.analysis.domain.AnalysisJob;
import com.panelvault.backend.analysis.domain.AnalysisPreset;
import com.panelvault.backend.analysis.domain.AnalysisResult;
import com.panelvault.backend.analysis.domain.JobStatus;
import com.panelvault.backend.analysis.domain.PageHash;
import com.panelvault.backend.analysis.domain.TestImages;
import com.panelvault.backend.identity.domain.DisplayName;
import com.panelvault.backend.identity.domain.Email;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.identity.infrastructure.persistence.JpaUserRepositoryAdapter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integracion de la cola y la cache contra PostgreSQL (base panelvault_test). Verifica
 * la consulta nativa {@code FOR UPDATE SKIP LOCKED}, la columna {@code bytea} y la clave compuesta.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaAnalysisRepositoriesTest {

    /** Fecha fija para que las horas sean predecibles. Las aserciones usan "contiene", asi no dependen de otros datos. */
    private static final Instant AHORA = Instant.parse("2090-01-01T12:00:00.123456Z");
    private static final Duration LEASE = Duration.ofMinutes(3);

    @Autowired
    private JpaAnalysisJobRepositoryAdapter jobs;

    @Autowired
    private JpaPageImageStore images;

    @Autowired
    private JpaAnalysisResultRepositoryAdapter results;

    @Autowired
    private JpaUserRepositoryAdapter users;

    private UserId usuario;

    @BeforeEach
    void setUp() {
        User user = users.save(User.register(
                new Email("cola-" + UUID.randomUUID() + "@panelvault.test"),
                new DisplayName("Usuario de Prueba"),
                "$2a$12$hashdeprueba",
                AHORA));
        usuario = user.id();
    }

    private AnalysisJob encolar(int seed, Instant cuando) {
        return jobs.save(AnalysisJob.enqueue(
                PageHash.of(TestImages.pngVariant(seed)), AnalysisPreset.WESTERN, usuario, cuando));
    }

    private List<UUID> idsTomables(Instant cuando) {
        return jobs.lockClaimable(50, cuando).stream().map(AnalysisJob::id).toList();
    }

    @Test
    void guardaYLeeUnTrabajoConTodosSusDatos() {
        AnalysisJob original = encolar(1, AHORA);

        AnalysisJob leido = jobs.findById(original.id()).orElseThrow();
        assertThat(leido.pageHash()).isEqualTo(original.pageHash());
        assertThat(leido.preset()).isEqualTo(AnalysisPreset.WESTERN);
        assertThat(leido.requestedBy()).isEqualTo(usuario);
        assertThat(leido.status()).isEqualTo(JobStatus.PENDING);
        assertThat(leido.availableAt()).isEqualTo(AHORA);
    }

    @Test
    void laConsultaDeLaColaRespetaLaHoraDisponibleYElLease() {
        AnalysisJob disponible = encolar(1, AHORA);
        AnalysisJob futuro = encolar(2, AHORA.plusSeconds(600));
        AnalysisJob enCurso = encolar(3, AHORA);
        enCurso.claim(AHORA, LEASE);
        jobs.save(enCurso);

        assertThat(idsTomables(AHORA)).contains(disponible.id()).doesNotContain(futuro.id(), enCurso.id());
        // Cuando vence el lease, el trabajo en curso vuelve a ser tomable.
        assertThat(idsTomables(AHORA.plus(LEASE).plusSeconds(1))).contains(disponible.id(), enCurso.id());
    }

    @Test
    void encuentraElTrabajoActivoDeUnaPaginaYCuentaLosDelUsuario() {
        AnalysisJob job = encolar(1, AHORA);
        encolar(2, AHORA);

        assertThat(jobs.findActive(job.pageHash(), AnalysisPreset.WESTERN)).map(AnalysisJob::id).contains(job.id());
        assertThat(jobs.findActive(job.pageHash(), AnalysisPreset.MANGA)).isEmpty();
        assertThat(jobs.countActiveByUser(usuario)).isEqualTo(2);

        job.claim(AHORA, LEASE);
        job.succeed(AHORA);
        jobs.save(job);
        assertThat(jobs.countActiveByUser(usuario)).isEqualTo(1);
        assertThat(jobs.findActive(job.pageHash(), AnalysisPreset.WESTERN)).isEmpty();
    }

    @Test
    void guardaLeeYBorraLaImagenDeUnTrabajo() {
        AnalysisJob job = encolar(1, AHORA);
        images.save(job.id(), TestImages.PNG);

        assertThat(images.load(job.id())).hasValueSatisfying(bytes -> assertThat(bytes).containsExactly(TestImages.PNG));
        images.delete(job.id());
        assertThat(images.load(job.id())).isEmpty();
    }

    @Test
    void laCacheGuardaUnResultadoPorPaginaYModo() {
        PageHash hash = PageHash.of(TestImages.pngVariant(7));
        results.save(new AnalysisResult(hash, AnalysisPreset.MANGA, "{\"panelMap\":{}}", AHORA));

        Optional<AnalysisResult> manga = results.find(hash, AnalysisPreset.MANGA);
        assertThat(manga).isPresent();
        assertThat(manga.get().payloadJson()).isEqualTo("{\"panelMap\":{}}");
        assertThat(results.find(hash, AnalysisPreset.WESTERN)).isEmpty();

        // Guardar otra vez la misma pagina y modo reemplaza el resultado.
        results.save(new AnalysisResult(hash, AnalysisPreset.MANGA, "{\"panelMap\":{\"v\":2}}", AHORA));
        assertThat(results.find(hash, AnalysisPreset.MANGA).orElseThrow().payloadJson()).contains("\"v\":2");
    }
}
