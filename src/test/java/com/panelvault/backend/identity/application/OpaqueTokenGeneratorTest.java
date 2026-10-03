package com.panelvault.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OpaqueTokenGeneratorTest {

    private final OpaqueTokenGenerator generator = new OpaqueTokenGenerator();

    @Test
    void generaTokensDe43CaracteresSeguros() {
        // 32 bytes en Base64 sin relleno = ceil(32 * 4 / 3) = 43 caracteres.
        String token = generator.generate();
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void nuncaRepiteTokens() {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            vistos.add(generator.generate());
        }
        assertThat(vistos).hasSize(1000);
    }

    @Test
    void sha256CoincideConElVectorOficialDelEstandar() {
        // Vector de prueba de FIPS 180-2 para la cadena "abc".
        assertThat(OpaqueTokenGenerator.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void laHuellaTiene64CaracteresHexadecimales() {
        assertThat(OpaqueTokenGenerator.sha256Hex(generator.generate())).hasSize(64).matches("[0-9a-f]+");
    }
}