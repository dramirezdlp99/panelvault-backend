package com.panelvault.backend.reading.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.shared.error.InvalidInputException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadingProgressTest {

    private static final Instant T0 = Instant.parse("2026-10-03T15:00:00Z");

    private static ProgressUpdate cambio(int page, Instant when, String device) {
        return new ProgressUpdate(page, 0, false, when, device);
    }

    private ReadingProgress empezado() {
        return ReadingProgress.start(UserId.newId(), new ComicId(UUID.randomUUID()), 40, cambio(5, T0, "portatil"), T0);
    }

    @Test
    void unCambioMasRecienteSeAplica() {
        ReadingProgress progress = empezado();

        assertThat(progress.merge(cambio(12, T0.plusSeconds(60), "celular"), 40, T0.plusSeconds(61))).isTrue();
        assertThat(progress.currentPage()).isEqualTo(12);
        assertThat(progress.deviceId()).isEqualTo("celular");
    }

    @Test
    void unCambioViejoQueLlegaTardeNoPisaElActual() {
        ReadingProgress progress = empezado();
        progress.merge(cambio(30, T0.plusSeconds(300), "portatil"), 40, T0.plusSeconds(300));

        // El celular estuvo sin conexion y envia ahora lo que leyo hace rato.
        boolean aplicado = progress.merge(cambio(8, T0.plusSeconds(100), "celular"), 40, T0.plusSeconds(400));

        assertThat(aplicado).isFalse();
        assertThat(progress.currentPage()).isEqualTo(30);
    }

    @Test
    void conLaMismaFechaGanaSiempreElMismoDispositivo() {
        // Sin importar el orden de llegada, el resultado final es el mismo en todos lados.
        ReadingProgress a = empezado();
        a.merge(cambio(10, T0.plusSeconds(60), "celular"), 40, T0);
        a.merge(cambio(20, T0.plusSeconds(60), "tablet"), 40, T0);

        ReadingProgress b = empezado();
        b.merge(cambio(20, T0.plusSeconds(60), "tablet"), 40, T0);
        b.merge(cambio(10, T0.plusSeconds(60), "celular"), 40, T0);

        assertThat(a.currentPage()).isEqualTo(20).isEqualTo(b.currentPage());
        assertThat(a.deviceId()).isEqualTo("tablet").isEqualTo(b.deviceId());
    }

    @Test
    void sabeSiTerminoYCuantoLleva() {
        ReadingProgress progress = empezado();
        assertThat(progress.isFinished()).isFalse();
        assertThat(progress.percent()).isEqualTo(12.5);

        progress.merge(cambio(40, T0.plusSeconds(10), "portatil"), 40, T0.plusSeconds(10));
        assertThat(progress.isFinished()).isTrue();
        assertThat(progress.percent()).isEqualTo(100.0);
    }

    @Test
    void unaFechaDelFuturoSeLimitaALaHoraDelServidor() {
        ProgressUpdate adelantado = cambio(3, T0.plusSeconds(86_400 * 365), "reloj-malo");

        assertThat(adelantado.clampedTo(T0).clientUpdatedAt()).isEqualTo(T0);
        assertThat(cambio(3, T0.minusSeconds(5), "ok").clampedTo(T0).clientUpdatedAt()).isEqualTo(T0.minusSeconds(5));
    }

    @Test
    void validaPaginaVinetaYDispositivo() {
        assertThatThrownBy(() -> cambio(0, T0, "x")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new ProgressUpdate(1, -1, false, T0, "x")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> cambio(1, T0, "con espacios")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> empezado().merge(cambio(41, T0.plusSeconds(5), "x"), 40, T0))
                .isInstanceOf(InvalidInputException.class);
    }
}
