package com.panelvault.backend.edge;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS de la API. El navegador normalmente NO llama al backend: llama al servidor de Next.js, que
 * reenvia la peticion (patron BFF). Aun asi se configura de forma explicita y cerrada: solo los
 * origenes listados, sin cookies ({@code allowCredentials=false}, la credencial va en la cabecera
 * Authorization) y exponiendo las cabeceras que el cliente necesita leer.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EdgeProperties.class)
public class EdgeConfiguration {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(EdgeProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.cors().allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                RequestSignatureVerifier.TIMESTAMP_HEADER,
                RequestSignatureVerifier.SIGNATURE_HEADER));
        cors.setExposedHeaders(List.of("Location", "Retry-After"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
