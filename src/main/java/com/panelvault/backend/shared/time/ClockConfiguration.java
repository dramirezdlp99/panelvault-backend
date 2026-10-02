package com.panelvault.backend.shared.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicacion como bean.
 *
 * <p>Nadie llama a {@code Instant.now()} directamente: se inyecta este {@link Clock}. En las
 * pruebas se usa un reloj fijo y las fechas se vuelven predecibles. Se usa UTC; la zona horaria
 * del usuario es un asunto de presentacion del frontend.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}