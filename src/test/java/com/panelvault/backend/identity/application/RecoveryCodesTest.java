package com.panelvault.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecoveryCodesTest {

    @Test
    void generaDiezCodigosDistintosConFormatoLegible() {
        List<String> codes = new RecoveryCodes().generate();

        assertThat(codes).hasSize(10).doesNotHaveDuplicates();
        assertThat(codes).allMatch(code -> code.matches("[2-9A-HJ-NP-Z]{5}-[2-9A-HJ-NP-Z]{5}"));
    }

    @Test
    void normalizaMinusculasGuionesYEspacios() {
        assertThat(RecoveryCodes.normalize(" abcde-fghjk ")).isEqualTo("ABCDEFGHJK");
        assertThat(RecoveryCodes.normalize(null)).isEmpty();
    }

    @Test
    void distingueUnCodigoDeRecuperacionDeUnCodigoTotp() {
        assertThat(RecoveryCodes.looksLikeRecoveryCode("abcde-fghjk")).isTrue();
        assertThat(RecoveryCodes.looksLikeRecoveryCode("123456")).isFalse();
        // La O y el 0 no existen en el alfabeto: un codigo con ellas no es de recuperacion.
        assertThat(RecoveryCodes.looksLikeRecoveryCode("ABCDE-FGHJO")).isFalse();
    }
}