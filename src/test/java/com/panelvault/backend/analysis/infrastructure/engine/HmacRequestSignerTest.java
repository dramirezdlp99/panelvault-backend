package com.panelvault.backend.analysis.infrastructure.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Los valores esperados se calcularon con la implementacion del motor de IA
 * ({@code panelvault_ai.api.security.sign}): si coinciden, el motor aceptara las firmas del backend.
 */
class HmacRequestSignerTest {

    private static final String SECRETO = "panelvault-test-only-engine-secret-0123456789";
    private final HmacRequestSigner signer = new HmacRequestSigner(SECRETO);

    @Test
    void elTextoCanonicoEsIgualAlDelMotor() {
        assertThat(HmacRequestSigner.canonicalMessage(
                        1_700_000_000L, "POST", "/v1/analyze", "imagen-de-prueba".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("1700000000\nPOST\n/v1/analyze\n"
                        + "34157b5820838f9aad44d4c639d4a2330dcb392ea75d2cfb659ede4ce37c7caf");
    }

    @Test
    void laFirmaCoincideConLaDelMotorEnPython() {
        assertThat(signer.sign(1_700_000_000L, "POST", "/v1/analyze", "imagen-de-prueba".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("8959d96aebc0fee841f714219d579bc0361bf6535028db0304cd5a7a6d292d9e");
    }

    @Test
    void elMetodoSeNormalizaAMayusculasYElCuerpoVacioTambienSeFirma() {
        assertThat(signer.sign(1_700_000_000L, "post", "/v1/analyze", new byte[0]))
                .isEqualTo("29d47cb8ffec879dc338ad1a475284e9d3d48962387fee628b15561497e216b6");
    }

    @Test
    void cambiarUnSoloByteCambiaLaFirma() {
        byte[] a = "pagina".getBytes(StandardCharsets.UTF_8);
        byte[] b = "pagina!".getBytes(StandardCharsets.UTF_8);
        assertThat(signer.sign(1L, "POST", "/v1/analyze", a)).isNotEqualTo(signer.sign(1L, "POST", "/v1/analyze", b));
    }

    @Test
    void exigeUnSecretoDeAlMenos32Caracteres() {
        assertThatThrownBy(() -> new HmacRequestSigner("corto")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new HmacRequestSigner(null)).isInstanceOf(IllegalStateException.class);
    }
}
