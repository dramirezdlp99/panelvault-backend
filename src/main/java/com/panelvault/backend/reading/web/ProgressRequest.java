package com.panelvault.backend.reading.web;

import com.panelvault.backend.reading.domain.ProgressUpdate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Cuerpo JSON de {@code PUT /api/v1/reading/comics/{id}/progress}. */
public record ProgressRequest(
        @NotNull @Min(1) Integer currentPage,
        @Min(0) @Max(500) Integer currentPanel,
        Boolean guidedMode,
        @NotNull Instant clientUpdatedAt,
        @NotBlank @Size(max = 64) String deviceId) {

    ProgressUpdate toUpdate() {
        return new ProgressUpdate(
                currentPage,
                currentPanel == null ? 0 : currentPanel,
                Boolean.TRUE.equals(guidedMode),
                clientUpdatedAt,
                deviceId);
    }
}
