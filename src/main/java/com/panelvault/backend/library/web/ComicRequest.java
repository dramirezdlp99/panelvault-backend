package com.panelvault.backend.library.web;

import com.panelvault.backend.library.domain.ComicDetails;
import com.panelvault.backend.library.domain.ComicFormat;
import com.panelvault.backend.library.domain.FileFingerprint;
import com.panelvault.backend.library.domain.ReadingDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Cuerpo JSON de {@code PUT /api/v1/library/comics/{id}}. */
public record ComicRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 200) String series,
        @Size(max = 20) String issueNumber,
        @NotNull @Min(1) @Max(2000) Integer pageCount,
        @NotNull ComicFormat format,
        @NotBlank @Size(max = 64) String fileSha256,
        ReadingDirection readingDirection,
        @Size(max = 10) List<String> tags) {

    ComicDetails toDetails() {
        return new ComicDetails(
                title, series, issueNumber, pageCount, format, new FileFingerprint(fileSha256), readingDirection, tags);
    }
}
