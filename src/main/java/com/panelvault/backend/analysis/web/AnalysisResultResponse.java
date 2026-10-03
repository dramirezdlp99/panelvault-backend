package com.panelvault.backend.analysis.web;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.panelvault.backend.analysis.domain.AnalysisResult;

/**
 * Mapa de vinetas de una pagina.
 *
 * <p>{@code result} es la respuesta del motor de IA y se envia tal cual, como JSON anidado
 * ({@link JsonRawValue}), sin convertirla a objetos Java y de vuelta a texto.
 */
public record AnalysisResultResponse(String pageSha256, String preset, @JsonRawValue String result) {

    public static AnalysisResultResponse from(AnalysisResult result) {
        return new AnalysisResultResponse(result.pageHash().value(), result.preset().value(), result.payloadJson());
    }
}
