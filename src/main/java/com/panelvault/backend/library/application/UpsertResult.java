package com.panelvault.backend.library.application;

import com.panelvault.backend.library.domain.Comic;

/** Resultado de guardar un comic: el comic y si se creo (201) o se actualizo (200). */
public record UpsertResult(Comic comic, boolean created) {}
