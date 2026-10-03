package com.panelvault.backend.reading.application;

import com.panelvault.backend.library.domain.Comic;
import com.panelvault.backend.reading.domain.ReadingProgress;

/** Un comic leido recientemente junto con su progreso ("continuar leyendo"). */
public record RecentReading(Comic comic, ReadingProgress progress) {}
