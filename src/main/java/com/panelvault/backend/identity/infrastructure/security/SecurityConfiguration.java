package com.panelvault.backend.identity.infrastructure.security;

import com.panelvault.backend.edge.EdgeProperties;
import com.panelvault.backend.edge.GatewaySignatureFilter;
import com.panelvault.backend.edge.RequestSignatureVerifier;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Reglas de seguridad HTTP del backend (RBAC).
 *
 * <ul>
 *   <li><b>Sin estado:</b> no hay sesion de servidor ni cookies; cada peticion trae su JWT.</li>
 *   <li><b>CSRF desactivado:</b> CSRF abusa de cookies que el navegador envia solo. Aqui la
 *       credencial va en la cabecera Authorization, que el navegador nunca agrega por su cuenta.
 *       Las cookies httpOnly las maneja el frontend (Next.js), que si se protegera de CSRF.</li>
 *   <li><b>Cerrado por defecto:</b> todo exige autenticacion salvo lo que se abre explicitamente.
 *       El segundo paso del login (2FA) es publico porque aun no hay sesion: lo protege su propio
 *       ticket temporal. El catalogo de obras libres es publico para que lo indexen los buscadores.</li>
 *   <li><b>Jerarquia de roles:</b> ADMIN incluye a CURADOR, que incluye a LECTOR.</li>
 *   <li><b>Gateway (opcional):</b> con {@code panelvault.edge.gateway.enabled=true}, toda peticion
 *       debe venir firmada por el servidor de Next.js, antes incluso de mirar el JWT.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain apiSecurity(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            SecurityErrorResponder errorResponder,
            EdgeProperties edge,
            Clock clock,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/2fa/verify")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health")
                        .permitAll()
                        // /error es a donde Spring reenvia los errores; sin esto, un 404 se volveria 401.
                        .requestMatchers("/error")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/curation/**")
                        .hasRole("CURADOR")
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errorResponder)
                        .accessDeniedHandler(errorResponder))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(errorResponder)
                        .accessDeniedHandler(errorResponder));

        EdgeProperties.Gateway gateway = edge.gateway();
        if (gateway.enabled()) {
            http.addFilterBefore(
                    new GatewaySignatureFilter(
                            new RequestSignatureVerifier(gateway.secret(), gateway.maxClockSkew()),
                            gateway.exemptPaths(),
                            gateway.maxBodyBytes(),
                            clock,
                            resolver),
                    SecurityContextHolderFilter.class);
        }
        return http.build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_CURADOR
                ROLE_CURADOR > ROLE_LECTOR
                """);
    }

    /** Lee el claim {@code role} del JWT y lo convierte en la autoridad {@code ROLE_<rol>}. */
    static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(JwtAccessTokenIssuer.ROLE_CLAIM);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
