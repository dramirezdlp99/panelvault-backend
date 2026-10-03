package com.panelvault.backend.shared.web;

import com.panelvault.backend.shared.error.DomainException;
import com.panelvault.backend.shared.error.ErrorCategory;
import com.panelvault.backend.shared.error.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce cualquier excepcion a un {@link ApiError} con el mismo formato en toda la API.
 *
 * <ul>
 *   <li>Errores de negocio ({@link DomainException}): el codigo HTTP sale de su categoria.</li>
 *   <li>Errores de Spring MVC (JSON mal formado, metodo no permitido, validacion...): se heredan de
 *       {@link ResponseEntityExceptionHandler} y solo se cambia el formato de la respuesta.</li>
 *   <li>Cualquier otro error: 500 con un mensaje generico y una referencia. El detalle va al log,
 *       nunca al cliente, porque podria revelar datos internos.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException ex, HttpServletRequest request) {
        HttpStatus status = statusFor(ex.category());
        ApiError body = ApiError.of(status.value(), ex.code(), ex.getMessage(), request.getRequestURI());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (ex instanceof TooManyRequestsException limited) {
            response.header(HttpHeaders.RETRY_AFTER, String.valueOf(limited.retryAfterSeconds()));
        }
        return response.body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        String reference = UUID.randomUUID().toString();
        log.error("Error inesperado [referencia={}] en {} {}", reference, request.getMethod(), request.getRequestURI(), ex);
        ApiError body = ApiError.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "server.unexpected_error",
                        "Ocurrio un error inesperado. Si persiste, reporta esta referencia.",
                        request.getRequestURI())
                .withReference(reference);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        ApiError body = ApiError.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "request.validation_failed",
                        "Algunos campos no son validos",
                        pathOf(request))
                .withFieldErrors(violations);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = detailOf(ex, body);
        ApiError error = ApiError.of(statusCode.value(), codeFor(statusCode), message, pathOf(request));
        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }

    /** Correspondencia entre la categoria de negocio y el codigo HTTP. */
    static HttpStatus statusFor(ErrorCategory category) {
        return switch (category) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case BUSINESS_RULE -> HttpStatus.valueOf(422);
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
        };
    }

    private static String codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "request.invalid";
            case 404 -> "resource.not_found";
            case 405 -> "request.method_not_allowed";
            case 406 -> "request.not_acceptable";
            case 415 -> "request.unsupported_media_type";
            default -> status.is4xxClientError() ? "request.error" : "server.error";
        };
    }

    private static String detailOf(Exception ex, Object body) {
        if (body instanceof ProblemDetail problem && problem.getDetail() != null) {
            return problem.getDetail();
        }
        if (ex instanceof ErrorResponse response && response.getBody().getDetail() != null) {
            return response.getBody().getDetail();
        }
        return "La peticion no pudo procesarse";
    }

    private static String pathOf(WebRequest request) {
        if (request instanceof ServletWebRequest servlet) {
            return servlet.getRequest().getRequestURI();
        }
        return null;
    }
}