package com.panelvault.backend.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Formato JSON uniforme de todas las respuestas de error del backend.
 *
 * <p>Los campos vacios no se envian: {@code reference} solo aparece en errores inesperados y
 * {@code fieldErrors} solo en errores de validacion.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        int status,
        String code,
        String message,
        String path,
        Instant timestamp,
        String reference,
        List<FieldViolation> fieldErrors) {

    /** Un campo concreto que no paso la validacion. */
    public record FieldViolation(String field, String message) {}

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(status, code, message, path, Instant.now(), null, List.of());
    }

    public ApiError withReference(String newReference) {
        return new ApiError(status, code, message, path, timestamp, newReference, fieldErrors);
    }

    public ApiError withFieldErrors(List<FieldViolation> violations) {
        return new ApiError(status, code, message, path, timestamp, reference, List.copyOf(violations));
    }
}