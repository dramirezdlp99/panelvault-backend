package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.PageImageStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Almacen de imagenes en memoria para pruebas (Fake). */
public class InMemoryPageImageStore implements PageImageStore {

    private final Map<UUID, byte[]> images = new HashMap<>();

    @Override
    public void save(UUID jobId, byte[] content) {
        images.put(jobId, content.clone());
    }

    @Override
    public Optional<byte[]> load(UUID jobId) {
        return Optional.ofNullable(images.get(jobId)).map(byte[]::clone);
    }

    @Override
    public void delete(UUID jobId) {
        images.remove(jobId);
    }

    public boolean contains(UUID jobId) {
        return images.containsKey(jobId);
    }
}
