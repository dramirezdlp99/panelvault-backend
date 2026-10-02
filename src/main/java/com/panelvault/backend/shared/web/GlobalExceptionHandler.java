package com.panelvault.backend.shared.web;

import com.panelvault.backend.shared.error.DomainException;
import com.panelvault.backend.shared.error.ErrorCategory;
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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce cualquier error a la respuesta uniforme {@link ApiError}.
 *
 * <p>Tres fuentes de errores:
 *
 * <ol>
 *   <li><b>Errores de negocio</b> ({@link DomainException}): su categoria define el codigo HTTP.
 *   <li><b>Errores del framework</b> (JSON mal formado, metodo no permitido, validacion): los
 *       detecta {@link ResponseEntityExceptionHandler} y aqui se convierten al formato uniforme.
 *   <li><b>Errores inesperados</b> (bugs): se registran completos en el log con una referencia,
 *       pero al cliente solo le llega un mensaje generico con esa referencia. Nunca detalles
 *       internos.
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException ex, HttpServletRequest request) {
        HttpStatus status = statusFor(ex.category());
        ApiError body = ApiError.of(status.value(), ex.code(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        String reference = UUID.randomUUID().toString();
        log.error(
                "Error inesperado [referencia={}] en {} {}",
                reference,
                request.getMethod(),
                request.getRequestURI(),
                ex);
        ApiError body = ApiError.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "server.unexpected_error",
                        "Ocurrio un error inesperado. Si persiste, reporta esta referencia.",
                        request.getRequestURI())
                .withReference(reference);
        return ResponseEntity.internalServerError().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        ApiError body = ApiError.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "request.validation_failed",
                        "Algunos campos no son validos",
                        pathOf(request))
                .withFieldErrors(violations);
        return ResponseEntity.badRequest().headers(headers).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        String message = (body instanceof ProblemDetail problem && problem.getDetail() != null)
                ? problem.getDetail()
                : "La peticion no pudo procesarse";
        ApiError error = ApiError.of(statusCode.value(), codeFor(statusCode), message, pathOf(request));
        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }

    /** Unico lugar donde una categoria de negocio se convierte en codigo HTTP. */
    private static HttpStatus statusFor(ErrorCategory category) {
        return switch (category) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            // 422: la peticion se entiende pero viola una regla. Se usa valueOf porque el nombre
            // de esta constante cambio entre versiones de Spring.
            case BUSINESS_RULE -> HttpStatus.valueOf(422);
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
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

    private static String pathOf(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }
        return null;
    }
}