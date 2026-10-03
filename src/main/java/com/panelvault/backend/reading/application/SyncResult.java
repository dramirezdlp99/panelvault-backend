package com.panelvault.backend.reading.application;

import com.panelvault.backend.reading.domain.ReadingProgress;

/**
 * Resultado de sincronizar el progreso: el estado que quedo en el servidor y si el cambio enviado
 * se aplico. Si no se aplico (habia uno mas reciente), el cliente debe adoptar {@code progress}.
 */
public record SyncResult(ReadingProgress progress, boolean applied) {}
