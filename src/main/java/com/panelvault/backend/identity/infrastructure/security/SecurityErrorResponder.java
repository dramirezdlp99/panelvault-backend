package com.panelvault.backend.identity.infrastructure.security;

import com.panelvault.backend.shared.error.DomainException;
import com.panelvault.backend.shared.error.ForbiddenException;
import com.panelvault.backend.shared.error.UnauthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Convierte los rechazos de Spring Security (401 y 403) en el mismo JSON de error que el resto de
 * la API.
 *
 * <p>Spring Security actua en un filtro, antes de llegar a los controladores, asi que el manejador
 * global no ve esos errores. Esta clase los traduce a excepciones de negocio y se los entrega al
 * {@link HandlerExceptionResolver} de Spring MVC, que si pasa por el manejador global. Resultado: un
 * solo formato de error en toda la aplicacion.
 */
@Component
public class SecurityErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public SecurityErrorResponder(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        // RFC 6750: un 401 de una API con tokens Bearer debe anunciar el esquema esperado.
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        UnauthenticatedException error = ex instanceof InvalidBearerTokenException
                ? new UnauthenticatedException("auth.invalid_token", "El token no es valido o ya vencio")
                : new UnauthenticatedException("auth.unauthenticated", "Necesitas iniciar sesion");
        respond(request, response, error, HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        respond(
                request,
                response,
                new ForbiddenException("auth.forbidden", "No tienes permiso para realizar esta accion"),
                HttpServletResponse.SC_FORBIDDEN);
    }

    private void respond(HttpServletRequest request, HttpServletResponse response, DomainException error, int status)
            throws IOException {
        if (resolver.resolveException(request, response, null, error) == null) {
            // Respaldo: si por algun motivo nadie la manejo, al menos se responde el codigo correcto.
            response.sendError(status);
        }
    }
}