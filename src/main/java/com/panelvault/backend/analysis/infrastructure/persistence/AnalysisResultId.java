package com.panelvault.backend.analysis.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de {@link AnalysisResultJpaEntity}: huella de la pagina y modo de lectura. */
public class AnalysisResultId implements Serializable {

    private String pageHash;
    private String preset;

    /** Requerido por JPA. */
    protected AnalysisResultId() {}

    public AnalysisResultId(String pageHash, String preset) {
        this.pageHash = pageHash;
        this.preset = preset;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof AnalysisResultId that
                        && Objects.equals(pageHash, that.pageHash)
                        && Objects.equals(preset, that.preset));
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageHash, preset);
    }
}
