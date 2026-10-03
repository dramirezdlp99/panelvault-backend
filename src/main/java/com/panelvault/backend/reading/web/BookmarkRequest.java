package com.panelvault.backend.reading.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo JSON de {@code PUT /api/v1/reading/comics/{comicId}/bookmarks/{bookmarkId}}. */
public record BookmarkRequest(@NotNull @Min(1) Integer page, @Size(max = 200) String note) {}
