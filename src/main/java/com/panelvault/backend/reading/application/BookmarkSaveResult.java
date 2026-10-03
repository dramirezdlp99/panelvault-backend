package com.panelvault.backend.reading.application;

import com.panelvault.backend.reading.domain.Bookmark;

/** Resultado de guardar un marcador: el marcador y si se creo (201) o se actualizo (200). */
public record BookmarkSaveResult(Bookmark bookmark, boolean created) {}
