-- V3: verificacion en dos pasos (2FA) con app autenticadora (TOTP).
--
-- secret_ciphertext: secreto TOTP cifrado con AES-256-GCM (nunca en claro).
-- recovery_codes: huellas HMAC-SHA256 de los codigos de recuperacion, separadas
--   por comas; cada codigo usado se elimina de la lista.
-- last_used_step: ultimo intervalo de 30 s aceptado; impide reusar un codigo.
-- enabled_at: NULL mientras la activacion esta pendiente de confirmar.

CREATE TABLE user_two_factor (
                                 user_id           UUID         NOT NULL,
                                 secret_ciphertext VARCHAR(200) NOT NULL,
                                 recovery_codes    VARCHAR(700) NOT NULL DEFAULT '',
                                 last_used_step    BIGINT,
                                 created_at        TIMESTAMPTZ  NOT NULL,
                                 enabled_at        TIMESTAMPTZ,
                                 CONSTRAINT pk_user_two_factor PRIMARY KEY (user_id),
                                 CONSTRAINT fk_user_two_factor_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);