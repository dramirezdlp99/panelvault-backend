package com.panelvault.backend.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.shared.crypto.Base32;
import com.panelvault.backend.shared.error.BusinessRuleException;
import com.panelvault.backend.shared.error.ConflictException;
import com.panelvault.backend.shared.error.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TwoFactorSetupServiceTest {

    private AuthFixture f;
    private User peter;

    @BeforeEach
    void setUp() {
        f = new AuthFixture();
        peter = f.registrar("peter@dailybugle.com");
    }

    @Test
    void alInicioLaVerificacionEnDosPasosEstaDesactivada() {
        assertThat(f.twoFactorSetup.status(peter.id())).isEqualTo(new TwoFactorStatus(false, 0));
    }

    @Test
    void iniciarDevuelveElSecretoYLaUriParaElQr() {
        TwoFactorEnrollment enrollment = f.twoFactorSetup.begin(peter.id());

        // 20 bytes en Base32 = 32 caracteres.
        assertThat(enrollment.secretBase32()).hasSize(32).matches("[A-Z2-7]+");
        assertThat(enrollment.otpAuthUri())
                .startsWith("otpauth://totp/PanelVault:peter%40dailybugle.com?")
                .contains("secret=" + enrollment.secretBase32());
        // Aun no esta activa: falta confirmar con un codigo.
        assertThat(f.twoFactorSetup.status(peter.id()).enabled()).isFalse();
    }

    @Test
    void elSecretoSeGuardaCifradoYLigadoAlUsuario() {
        TwoFactorEnrollment enrollment = f.twoFactorSetup.begin(peter.id());

        String guardado = f.twoFactor.findByUserId(peter.id()).orElseThrow().encryptedSecret();
        assertThat(guardado).startsWith("enc:" + peter.id() + ":").doesNotContain(enrollment.secretBase32());
    }

    @Test
    void confirmarConUnCodigoCorrectoActivaYEntregaDiezCodigosDeRecuperacion() {
        AuthFixture.Activation activation = f.activarDosPasos(peter);

        assertThat(activation.recoveryCodes()).hasSize(10).doesNotHaveDuplicates();
        assertThat(f.twoFactorSetup.status(peter.id())).isEqualTo(new TwoFactorStatus(true, 10));
    }

    @Test
    void unCodigoIncorrectoNoActiva() {
        f.twoFactorSetup.begin(peter.id());

        assertThatThrownBy(() -> f.twoFactorSetup.confirm(peter.id(), "000000"))
                .isInstanceOf(InvalidInputException.class)
                .extracting("code")
                .isEqualTo("auth.invalid_2fa_code");
        assertThat(f.twoFactorSetup.status(peter.id()).enabled()).isFalse();
    }

    @Test
    void confirmarSinHaberIniciadoNoTieneSentido() {
        assertThatThrownBy(() -> f.twoFactorSetup.confirm(peter.id(), "123456"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("auth.2fa_not_started");
    }

    @Test
    void noSePuedeIniciarOtraVezSiYaEstaActiva() {
        f.activarDosPasos(peter);

        assertThatThrownBy(() -> f.twoFactorSetup.begin(peter.id()))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo("auth.2fa_already_enabled");
    }

    @Test
    void iniciarDeNuevoAntesDeConfirmarReemplazaElSecretoPendiente() {
        String primero = f.twoFactorSetup.begin(peter.id()).secretBase32();
        String segundo = f.twoFactorSetup.begin(peter.id()).secretBase32();

        assertThat(segundo).isNotEqualTo(primero);
        // El codigo del primer secreto ya no sirve para confirmar.
        String codigoViejo = f.codigoActual(Base32.decode(primero));
        assertThatThrownBy(() -> f.twoFactorSetup.confirm(peter.id(), codigoViejo))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void desactivarConUnCodigoDeLaApp() {
        AuthFixture.Activation activation = f.activarDosPasos(peter);

        f.twoFactorSetup.disable(peter.id(), f.codigoSiguiente(activation.secret()));

        assertThat(f.twoFactorSetup.status(peter.id()).enabled()).isFalse();
    }

    @Test
    void desactivarConUnCodigoDeRecuperacion() {
        AuthFixture.Activation activation = f.activarDosPasos(peter);

        f.twoFactorSetup.disable(peter.id(), activation.recoveryCodes().get(3));

        assertThat(f.twoFactorSetup.status(peter.id()).enabled()).isFalse();
    }

    @Test
    void desactivarConUnCodigoIncorrectoNoHaceNada() {
        f.activarDosPasos(peter);

        assertThatThrownBy(() -> f.twoFactorSetup.disable(peter.id(), "999999"))
                .isInstanceOf(InvalidInputException.class);
        assertThat(f.twoFactorSetup.status(peter.id()).enabled()).isTrue();
    }

    @Test
    void desactivarSinTenerlaActivaNoTieneSentido() {
        assertThatThrownBy(() -> f.twoFactorSetup.disable(peter.id(), "123456"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("auth.2fa_not_enabled");
    }
}