package com.panelvault.backend.edge;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades {@code panelvault.edge.*}: la capa de borde (Edge/Gateway) de la arquitectura.
 *
 * @param gateway firma HMAC que exige el backend a quien lo llama (el servidor de Next.js)
 * @param cors    origenes de navegador autorizados
 */
@ConfigurationProperties(prefix = "panelvault.edge")
public record EdgeProperties(Gateway gateway, Cors cors) {

    /**
     * @param enabled      si se exige la firma (en produccion si; en local y en pruebas no)
     * @param secret       secreto compartido con el frontend (minimo 32 caracteres)
     * @param maxClockSkew antiguedad maxima de una peticion firmada
     * @param maxBodyBytes tamano maximo del cuerpo que se lee para verificar la firma
     * @param exemptPaths  rutas que no exigen firma (por ejemplo, la salud del servicio)
     */
    public record Gateway(
            boolean enabled, String secret, Duration maxClockSkew, long maxBodyBytes, List<String> exemptPaths) {

        @Override
        public String toString() {
            return "Gateway[enabled=" + enabled + ", exemptPaths=" + exemptPaths + ", secret=***]";
        }
    }

    /** @param allowedOrigins origenes permitidos, por ejemplo {@code http://localhost:3000} */
    public record Cors(List<String> allowedOrigins) {}
}
