package com.panelvault.backend.catalog.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.panelvault.backend.catalog.domain.CatalogWork;
import com.panelvault.backend.catalog.domain.WorkDetails;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Representacion de una obra del catalogo. Los campos vacios se omiten. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkResponse(
        UUID id,
        String slug,
        String title,
        String author,
        Integer year,
        String publisher,
        String description,
        String sourceUrl,
        String coverUrl,
        String license,
        Integer pageCount,
        List<String> tags,
        boolean published,
        Instant publishedAt,
        Instant updatedAt) {

    public static WorkResponse from(CatalogWork work) {
        WorkDetails d = work.details();
        return new WorkResponse(
                work.id(),
                work.slug().value(),
                d.title(),
                d.author(),
                d.year(),
                d.publisher(),
                d.description(),
                d.sourceUrl(),
                d.coverUrl(),
                d.license().name(),
                d.pageCount(),
                List.copyOf(d.tags()),
                work.published(),
                work.publishedAt(),
                work.updatedAt());
    }
}
