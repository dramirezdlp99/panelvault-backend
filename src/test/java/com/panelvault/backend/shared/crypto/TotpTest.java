package com.panelvault.backend.shared.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TotpTest {

    /** Secreto de los vectores de prueba de los RFC 4226 y 6238. */
    private static final byte[] SECRETO_RFC = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    @Test
    void hotpCoincideConLosVectoresDelRfc4226() {
        String[] esperados = {
                "755224", "287082", "359152", "969429", "338314", "254676", "287922", "162583", "399871", "520489"
        };
        for (int contador = 0; contador < esperados.length; contador++) {
            assertThat(Totp.hotp(SECRETO_RFC, contador, 6)).isEqualTo(esperados[contador]);
        }
    }

    @Test
    void totpCoincideConLosVectoresDelRfc6238() {
        // RFC 6238, apendice B, columna SHA1 (8 digitos).
        assertThat(totp8(59)).isEqualTo("94287082");
        assertThat(totp8(1111111109)).isEqualTo("07081804");
        assertThat(totp8(1111111111)).isEqualTo("14050471");
        assertThat(totp8(1234567890)).isEqualTo("89005924");
        assertThat(totp8(2000000000)).isEqualTo("69279037");
        assertThat(totp8(20000000000L)).isEqualTo("65353130");
    }

    private static String totp8(long epochSeconds) {
        return Totp.hotp(SECRETO_RFC, Totp.timeStep(Instant.ofEpochSecond(epochSeconds)), 8);
    }

    @Test
    void aceptaElIntervaloActualYLosVecinosPeroNoMasLejos() {
        Instant ahora = Instant.ofEpochSecond(1_700_000_000L);
        long actual = Totp.timeStep(ahora);

        assertThat(Totp.verify(SECRETO_RFC, Totp.code(SECRETO_RFC, actual), ahora, 1)).hasValue(actual);
        assertThat(Totp.verify(SECRETO_RFC, Totp.code(SECRETO_RFC, actual - 1), ahora, 1)).hasValue(actual - 1);
        assertThat(Totp.verify(SECRETO_RFC, Totp.code(SECRETO_RFC, actual + 1), ahora, 1)).hasValue(actual + 1);
        assertThat(Totp.verify(SECRETO_RFC, Totp.code(SECRETO_RFC, actual - 2), ahora, 1)).isEmpty();
    }

    @Test
    void rechazaCodigosMalFormados() {
        Instant ahora = Instant.ofEpochSecond(1_700_000_000L);
        assertThat(Totp.verify(SECRETO_RFC, null, ahora, 1)).isEmpty();
        assertThat(Totp.verify(SECRETO_RFC, "12345", ahora, 1)).isEmpty();
        assertThat(Totp.verify(SECRETO_RFC, "12a456", ahora, 1)).isEmpty();
    }

    @Test
    void generaSecretosDe160Bits() {
        assertThat(Totp.newSecret(new java.security.SecureRandom())).hasSize(20);
    }

    @Test
    void construyeLaUriQueEntiendenLasAppsAutenticadoras() {
        assertThat(Totp.otpAuthUri("PanelVault", "peter parker@dailybugle.com", SECRETO_RFC))
                .isEqualTo("otpauth://totp/PanelVault:peter%20parker%40dailybugle.com"
                        + "?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=PanelVault"
                        + "&algorithm=SHA1&digits=6&period=30");
    }
}