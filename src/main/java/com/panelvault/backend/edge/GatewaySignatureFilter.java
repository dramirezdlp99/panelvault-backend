package com.panelvault.backend.edge;

import com.panelvault.backend.shared.error.DomainException;
import com.panelvault.backend.shared.error.InvalidInputException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.List;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Filtro de la capa Edge/Gateway: exige que cada peticion venga firmada por el servidor de Next.js.
 *
 * <p>Corre antes que Spring Security. Si la firma falla responde 401 con el mismo JSON de error que
 * el resto de la API (se lo delega al manejador global, igual que los errores de seguridad).
 */
public class GatewaySignatureFilter extends OncePerRequestFilter {

    private final RequestSignatureVerifier verifier;
    private final List<String> exemptPaths;
    private final long maxBodyBytes;
    private final Clock clock;
    private final HandlerExceptionResolver resolver;

    public GatewaySignatureFilter(
            RequestSignatureVerifier verifier,
            List<String> exemptPaths,
            long maxBodyBytes,
            Clock clock,
            HandlerExceptionResolver resolver) {
        this.verifier = verifier;
        this.exemptPaths = List.copyOf(exemptPaths);
        this.maxBodyBytes = maxBodyBytes;
        this.clock = clock;
        this.resolver = resolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Las preflight de CORS las responde el navegador sin cuerpo ni firma.
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || exemptPaths.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] body;
        try {
            body = readLimited(request);
            verifier.verify(
                    request.getHeader(RequestSignatureVerifier.TIMESTAMP_HEADER),
                    request.getHeader(RequestSignatureVerifier.SIGNATURE_HEADER),
                    request.getMethod(),
                    pathAndQuery(request),
                    body,
                    clock.instant());
        } catch (DomainException e) {
            if (resolver.resolveException(request, response, null, e) == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
            return;
        }
        chain.doFilter(new CachedBodyHttpServletRequest(request, body), response);
    }

    private byte[] readLimited(HttpServletRequest request) throws IOException {
        if (request.getContentLengthLong() > maxBodyBytes) {
            throw tooLarge();
        }
        try (InputStream in = request.getInputStream()) {
            byte[] data = in.readNBytes((int) Math.min(maxBodyBytes + 1, Integer.MAX_VALUE));
            if (data.length > maxBodyBytes) {
                throw tooLarge();
            }
            return data;
        }
    }

    private static InvalidInputException tooLarge() {
        return new InvalidInputException("gateway.body_too_large", "El cuerpo de la peticion es demasiado grande");
    }

    static String pathAndQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null || query.isEmpty() ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }
}
