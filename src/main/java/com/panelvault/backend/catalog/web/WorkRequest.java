package com.panelvault.backend.catalog.web;

import com.panelvault.backend.catalog.domain.License;
import com.panelvault.backend.catalog.domain.WorkDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Cuerpo JSON para crear o editar una obra del catalogo. */
public record WorkRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 120) String author,
        Integer year,
        @Size(max = 120) String publisher,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 500) String sourceUrl,
        @Size(max = 500) String coverUrl,
        @NotNull License license,
        Integer pageCount,
        @Size(max = 10) List<String> tags) {

    WorkDetails toDetails() {
        return new WorkDetails(title, author, year, publisher, description, sourceUrl, coverUrl, license, pageCount, tags);
    }
}
