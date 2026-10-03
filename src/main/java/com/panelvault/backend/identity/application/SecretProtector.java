package com.panelvault.backend.identity.application;

/**
 * Puerto para proteger secretos que el servidor necesita guardar.
 *
 * <ul>
 *   <li>El secreto TOTP no puede guardarse con hash como una contrasena, porque hay que leerlo para
 *       calcular los codigos. Se guarda <b>cifrado</b>, y {@code context} (el id del usuario) queda
 *       ligado al cifrado: el secreto de un usuario copiado en la fila de otro no se descifra.</li>
 *   <li>Los codigos de recuperacion si basta con compararlos, asi que se guarda solo su
 *       <b>huella</b> con clave (HMAC). Sin la clave del servidor, la base robada no sirve para
 *       adivinarlos.</li>
 * </ul>
 */
public interface SecretProtector {

    String encrypt(byte[] plaintext, String context);

    /** @throws IllegalStateException si el texto fue alterado o el contexto no coincide */
    byte[] decrypt(String ciphertext, String context);

    String fingerprint(String value);
}