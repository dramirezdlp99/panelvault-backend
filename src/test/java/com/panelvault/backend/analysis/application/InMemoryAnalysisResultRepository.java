package com.panelvault.backend.analysis.application;

import com.panelvault.backend.analysis.domain.AnalysisPreset;
import com.panelvault.backend.analysis.domain.AnalysisResult;
import com.panelvault.backend.analysis.domain.AnalysisResultRepository;
import com.panelvault.backend.analysis.domain.PageHash;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Cache de resultados en memoria para pruebas (Fake). */
public class InMemoryAnalysisResultRepository implements AnalysisResultRepository {

    private final Map<String, AnalysisResult> results = new HashMap<>();

    @Override
    public Optional<AnalysisResult> find(PageHash pageHash, AnalysisPreset preset) {
        return Optional.ofNullable(results.get(key(pageHash, preset)));
    }

    @Override
    public AnalysisResult save(AnalysisResult result) {
        results.put(key(result.pageHash(), result.preset()), result);
        return result;
    }

    public int count() {
        return results.size();
    }

    private static String key(PageHash pageHash, AnalysisPreset preset) {
        return pageHash.value() + ":" + preset.value();
    }
}
