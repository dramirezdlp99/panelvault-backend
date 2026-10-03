-- V5: biblioteca de comics de cada usuario (solo metadatos).
--
-- El archivo del comic y sus paginas viven en el navegador (IndexedDB): aqui
-- solo se guarda lo necesario para ver la misma biblioteca en todos los
-- dispositivos. El id lo genera el cliente (sincronizacion offline idempotente).
-- file_sha256 identifica el archivo: el mismo usuario no puede tenerlo dos veces.
-- tags: etiquetas en minusculas separadas por comas.

CREATE TABLE comics (
    id                UUID         NOT NULL,
    owner_id          UUID         NOT NULL,
    title             VARCHAR(200) NOT NULL,
    series            VARCHAR(200),
    issue_number      VARCHAR(20),
    page_count        INTEGER      NOT NULL,
    format            VARCHAR(10)  NOT NULL,
    file_sha256       VARCHAR(64)  NOT NULL,
    reading_direction VARCHAR(16)  NOT NULL,
    tags              VARCHAR(400) NOT NULL DEFAULT '',
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    version           BIGINT       NOT NULL,
    CONSTRAINT pk_comics PRIMARY KEY (id),
    CONSTRAINT fk_comics_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_comics_owner_file UNIQUE (owner_id, file_sha256),
    CONSTRAINT ck_comics_page_count CHECK (page_count BETWEEN 1 AND 2000),
    CONSTRAINT ck_comics_format CHECK (format IN ('CBZ', 'CBR', 'PDF', 'IMAGES')),
    CONSTRAINT ck_comics_direction CHECK (reading_direction IN ('LEFT_TO_RIGHT', 'RIGHT_TO_LEFT')),
    CONSTRAINT ck_comics_sha256 CHECK (file_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_comics_version CHECK (version >= 1)
);

-- El listado se ordena por ultima modificacion dentro de la biblioteca de cada usuario.
CREATE INDEX ix_comics_owner_updated ON comics (owner_id, updated_at DESC);
