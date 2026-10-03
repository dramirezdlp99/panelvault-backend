-- V6: progreso de lectura y marcadores, sincronizados entre dispositivos.
--
-- reading_progress: una fila por (usuario, comic). client_updated_at y device_id
-- deciden que cambio gana cuando dos dispositivos sincronizan (el ultimo gana).
-- current_panel guarda la vineta actual para retomar la Lectura Guiada.
-- Al borrar un comic se borran su progreso y sus marcadores (ON DELETE CASCADE).

CREATE TABLE reading_progress (
    user_id           UUID        NOT NULL,
    comic_id          UUID        NOT NULL,
    current_page      INTEGER     NOT NULL,
    total_pages       INTEGER     NOT NULL,
    current_panel     INTEGER     NOT NULL DEFAULT 0,
    guided_mode       BOOLEAN     NOT NULL DEFAULT FALSE,
    finished          BOOLEAN     NOT NULL DEFAULT FALSE,
    client_updated_at TIMESTAMPTZ NOT NULL,
    device_id         VARCHAR(64) NOT NULL,
    server_updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_reading_progress PRIMARY KEY (user_id, comic_id),
    CONSTRAINT fk_reading_progress_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_progress_comic FOREIGN KEY (comic_id) REFERENCES comics (id) ON DELETE CASCADE,
    CONSTRAINT ck_reading_progress_page CHECK (current_page >= 1 AND current_page <= total_pages),
    CONSTRAINT ck_reading_progress_panel CHECK (current_panel >= 0)
);

CREATE INDEX ix_reading_progress_recent ON reading_progress (user_id, server_updated_at DESC);

CREATE TABLE bookmarks (
    id         UUID         NOT NULL,
    user_id    UUID         NOT NULL,
    comic_id   UUID         NOT NULL,
    page       INTEGER      NOT NULL,
    note       VARCHAR(200) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_bookmarks PRIMARY KEY (id),
    CONSTRAINT fk_bookmarks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmarks_comic FOREIGN KEY (comic_id) REFERENCES comics (id) ON DELETE CASCADE,
    CONSTRAINT ck_bookmarks_page CHECK (page >= 1)
);

CREATE INDEX ix_bookmarks_comic ON bookmarks (user_id, comic_id, page);
