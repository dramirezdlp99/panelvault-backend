package com.panelvault.backend.shared.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.Test;

class Base32Test {

    private static String encode(String text) {
        return Base32.encode(text.getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void codificaLosVectoresOficialesDelRfc4648() {
        // RFC 4648, seccion 10 (sin el relleno '=').
        assertThat(encode("")).isEqualTo("");
        assertThat(encode("f")).isEqualTo("MY");
        assertThat(encode("fo")).isEqualTo("MZXQ");
        assertThat(encode("foo")).isEqualTo("MZXW6");
        assertThat(encode("foob")).isEqualTo("MZXW6YQ");
        assertThat(encode("fooba")).isEqualTo("MZXW6YTB");
        assertThat(encode("foobar")).isEqualTo("MZXW6YTBOI");
    }

    @Test
    void decodificaConRellenoMinusculasEspaciosYGuiones() {
        assertThat(new String(Base32.decode("MZXW6YTBOI======"), StandardCharsets.US_ASCII)).isEqualTo("foobar");
        assertThat(new String(Base32.decode("mzxw 6ytb-oi"), StandardCharsets.US_ASCII)).isEqualTo("foobar");
    }

    @Test
    void idaYVueltaConservaCualquierSecuenciaDeBytes() {
        Random random = new Random(42);
        for (int length = 0; length <= 40; length++) {
            byte[] data = new byte[length];
            random.nextBytes(data);
            assertThat(Base32.decode(Base32.encode(data))).containsExactly(data);
        }
    }

    @Test
    void rechazaCaracteresFueraDelAlfabeto() {
        // 0, 1, 8 y 9 no existen en Base32: se excluyeron por parecerse a O, I y B.
        assertThatThrownBy(() -> Base32.decode("MZXW1")).isInstanceOf(IllegalArgumentException.class);
    }
}