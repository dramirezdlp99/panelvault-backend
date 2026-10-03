-- V2: refresh tokens con rotacion y deteccion de reuso.
--
-- token_hash guarda el SHA-256 en hexadecimal (64 caracteres), nunca el token.
-- family_id agrupa todos los tokens nacidos de un mismo login; si se detecta
-- reuso se revocan todos los de la familia con un solo UPDATE (por eso el indice).
-- replaced_by apunta al token que reemplazo a este al rotar (traza de auditoria).
-- Si se borra un usuario, sus tokens se borran con el (ON DELETE CASCADE).

CREATE TABLE refresh_tokens (
                                id          UUID        NOT NULL,
                                user_id     UUID        NOT NULL,
                                family_id   UUID        NOT NULL,
                                token_hash  VARCHAR(64) NOT NULL,
                                issued_at   TIMESTAMPTZ NOT NULL,
                                expires_at  TIMESTAMPTZ NOT NULL,
                                revoked_at  TIMESTAMPTZ,
                                replaced_by UUID,
                                CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
                                CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
                                CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                CONSTRAINT ck_refresh_tokens_expiry CHECK (expires_at > issued_at)
);

CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);