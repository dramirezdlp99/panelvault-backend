package com.panelvault.backend.edge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.shared.error.UnauthenticatedException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RequestSignatureVerifierTest {

    private static final String SECRETO = "panelvault-test-only-gateway-secret-0123456789";
    private static final Instant AHORA = Instant.ofEpochSecond(1_700_000_000L);
    private static final byte[] CUERPO = "{\"email\":\"a@b.co\"}".getBytes(StandardCharsets.UTF_8);

    private final RequestSignatureVerifier verifier = new RequestSignatureVerifier(SECRETO, Duration.ofMinutes(5));

    private String firma(String ts, String method, String path, byte[] body) {
        return verifier.sign(ts, method, path, body);
    }

    private String codigo(Runnable accion) {
        try {
            accion.run();
            return "valida";
        } catch (UnauthenticatedException e) {
            return e.code();
        }
    }

    @Test
    void elTextoCanonicoIncluyeLaQueryYElHashDelCuerpo() {
        assertThat(RequestSignatureVerifier.canonicalMessage("1700000000", "post", "/api/v1/analysis/pages?preset=manga",
                        new byte[0]))
                .isEqualTo("1700000000\nPOST\n/api/v1/analysis/pages?preset=manga\n"
                        + "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void aceptaUnaFirmaCorrectaAunqueVengaEnMayusculas() {
        String ts = "1700000000";
        String sig = firma(ts, "POST", "/api/v1/auth/login", CUERPO);

        assertThat(codigo(() -> verifier.verify(ts, sig, "POST", "/api/v1/auth/login", CUERPO, AHORA))).isEqualTo("valida");
        assertThat(codigo(() -> verifier.verify(ts, sig.toUpperCase(), "POST", "/api/v1/auth/login", CUERPO, AHORA)))
                .isEqualTo("valida");
    }

    @Test
    void cualquierCambioInvalidaLaFirma() {
        String ts = "1700000000";
        String sig = firma(ts, "POST", "/api/v1/analysis/pages?preset=western", CUERPO);

        assertThat(codigo(() -> verifier.verify(ts, sig, "POST", "/api/v1/analysis/pages?preset=manga", CUERPO, AHORA)))
                .isEqualTo("gateway.invalid_signature");
        assertThat(codigo(() -> verifier.verify(ts, sig, "PUT", "/api/v1/analysis/pages?preset=western", CUERPO, AHORA)))
                .isEqualTo("gateway.invalid_signature");
        byte[] otroCuerpo = "{\"email\":\"x@b.co\"}".getBytes(StandardCharsets.UTF_8);
        assertThat(codigo(() -> verifier.verify(ts, sig, "POST", "/api/v1/analysis/pages?preset=western", otroCuerpo, AHORA)))
                .isEqualTo("gateway.invalid_signature");
    }

    @Test
    void unaFirmaViejaODelFuturoExpira() {
        String viejo = Long.toString(AHORA.getEpochSecond() - 301);
        String futuro = Long.toString(AHORA.getEpochSecond() + 301);

        assertThat(codigo(() -> verifier.verify(viejo, firma(viejo, "GET", "/x", new byte[0]), "GET", "/x", new byte[0], AHORA)))
                .isEqualTo("gateway.expired_signature");
        assertThat(codigo(() -> verifier.verify(futuro, firma(futuro, "GET", "/x", new byte[0]), "GET", "/x", new byte[0], AHORA)))
                .isEqualTo("gateway.expired_signature");
    }

    @Test
    void sinCabecerasOConTimestampInvalidoSeRechaza() {
        assertThat(codigo(() -> verifier.verify(null, "abc", "GET", "/x", new byte[0], AHORA)))
                .isEqualTo("gateway.missing_signature");
        assertThat(codigo(() -> verifier.verify("1700000000", " ", "GET", "/x", new byte[0], AHORA)))
                .isEqualTo("gateway.missing_signature");
        assertThat(codigo(() -> verifier.verify("ayer", "abc", "GET", "/x", new byte[0], AHORA)))
                .isEqualTo("gateway.invalid_signature");
    }

    @Test
    void exigeUnSecretoDeAlMenos32Caracteres() {
        assertThatThrownBy(() -> new RequestSignatureVerifier("corto", Duration.ofMinutes(5)))
                .isInstanceOf(IllegalStateException.class);
    }
}
