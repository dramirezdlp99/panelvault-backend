package com.panelvault.backend.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesGcmSecretProtectorTest {

    private static String clave(String texto32) {
        return Base64.getEncoder().encodeToString(texto32.getBytes(StandardCharsets.US_ASCII));
    }

    private final AesGcmSecretProtector protector = new AesGcmSecretProtector(clave("clave-aes-de-prueba-32-bytes!!!!"));
    private final byte[] secreto = "secreto-totp-20bytes".getBytes(StandardCharsets.US_ASCII);

    @Test
    void cifraYDescifraElMismoSecreto() {
        String cifrado = protector.encrypt(secreto, "usuario-1");

        assertThat(protector.decrypt(cifrado, "usuario-1")).containsExactly(secreto);
        assertThat(cifrado).doesNotContain("secreto");
    }

    @Test
    void cifrarDosVecesDaResultadosDistintosPorElIvAleatorio() {
        assertThat(protector.encrypt(secreto, "usuario-1")).isNotEqualTo(protector.encrypt(secreto, "usuario-1"));
    }

    @Test
    void noSeDescifraConElContextoDeOtroUsuario() {
        String cifrado = protector.encrypt(secreto, "usuario-1");

        assertThatThrownBy(() -> protector.decrypt(cifrado, "usuario-2")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unTextoCifradoAlteradoSeDetecta() {
        byte[] datos = Base64.getDecoder().decode(protector.encrypt(secreto, "usuario-1"));
        datos[datos.length - 5] ^= 0x01;
        String alterado = Base64.getEncoder().encodeToString(datos);

        assertThatThrownBy(() -> protector.decrypt(alterado, "usuario-1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void otraClaveNoPuedeDescifrar() {
        String cifrado = protector.encrypt(secreto, "usuario-1");
        AesGcmSecretProtector otro = new AesGcmSecretProtector(clave("otra-clave-aes-distinta-32bytes!"));

        assertThatThrownBy(() -> otro.decrypt(cifrado, "usuario-1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void laHuellaEsDeterministaYNoRevelaElValor() {
        String huella = protector.fingerprint("ABCDEFGHJK");

        assertThat(huella).isEqualTo(protector.fingerprint("ABCDEFGHJK")).hasSize(64).matches("[0-9a-f]+");
        assertThat(huella).isNotEqualTo(protector.fingerprint("ABCDEFGHJM"));
        assertThat(huella).doesNotContain("ABCDEFGHJK");
    }

    @Test
    void laHuellaDependeDeLaClaveDelServidor() {
        AesGcmSecretProtector otro = new AesGcmSecretProtector(clave("otra-clave-aes-distinta-32bytes!"));
        assertThat(otro.fingerprint("ABCDEFGHJK")).isNotEqualTo(protector.fingerprint("ABCDEFGHJK"));
    }

    @Test
    void exigeUnaClaveBase64DeExactamente32Bytes() {
        assertThatThrownBy(() -> new AesGcmSecretProtector(clave("corta")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactamente 32 bytes");
        assertThatThrownBy(() -> new AesGcmSecretProtector("esto no es base64 !!"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new AesGcmSecretProtector(null)).isInstanceOf(IllegalStateException.class);
    }
}