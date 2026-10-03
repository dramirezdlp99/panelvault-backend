-- V4: cola de analisis de vinetas y cache de resultados.
--
-- analysis_jobs es una cola de trabajos sobre PostgreSQL. Los workers toman
-- trabajos con SELECT ... FOR UPDATE SKIP LOCKED, asi varios pueden trabajar en
-- paralelo sin tomar nunca el mismo. Un trabajo RUNNING con lease_until vencido
-- se considera abandonado (el worker murio) y puede retomarse.
--
-- analysis_job_images guarda la imagen SOLO mientras el trabajo esta activo;
-- se borra al terminar. Va en tabla aparte para no cargar bytes al consultar la cola.
--
-- analysis_results es la cache: una fila por (huella SHA-256 de la pagina, modo
-- de lectura). La misma pagina subida por dos usuarios se analiza una sola vez.

CREATE TABLE analysis_jobs (
    id           UUID         NOT NULL,
    page_hash    VARCHAR(64)  NOT NULL,
    preset       VARCHAR(16)  NOT NULL,
    requested_by UUID         NOT NULL,
    status       VARCHAR(16)  NOT NULL,
    attempts     INTEGER      NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ  NOT NULL,
    lease_until  TIMESTAMPTZ,
    last_error   VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_analysis_jobs PRIMARY KEY (id),
    CONSTRAINT fk_analysis_jobs_user FOREIGN KEY (requested_by) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_analysis_jobs_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_analysis_jobs_preset CHECK (preset IN ('western', 'manga')),
    CONSTRAINT ck_analysis_jobs_attempts CHECK (attempts >= 0),
    CONSTRAINT ck_analysis_jobs_hash CHECK (page_hash ~ '^[0-9a-f]{64}$')
);

-- Indices parciales: solo indexan las filas que el worker necesita encontrar.
CREATE INDEX ix_analysis_jobs_pending ON analysis_jobs (available_at) WHERE status = 'PENDING';
CREATE INDEX ix_analysis_jobs_running ON analysis_jobs (lease_until) WHERE status = 'RUNNING';
CREATE INDEX ix_analysis_jobs_page ON analysis_jobs (page_hash, preset);
CREATE INDEX ix_analysis_jobs_user_status ON analysis_jobs (requested_by, status);

CREATE TABLE analysis_job_images (
    job_id     UUID    NOT NULL,
    content    BYTEA   NOT NULL,
    size_bytes INTEGER NOT NULL,
    CONSTRAINT pk_analysis_job_images PRIMARY KEY (job_id),
    CONSTRAINT fk_analysis_job_images_job FOREIGN KEY (job_id) REFERENCES analysis_jobs (id) ON DELETE CASCADE,
    CONSTRAINT ck_analysis_job_images_size CHECK (size_bytes > 0)
);

CREATE TABLE analysis_results (
    page_hash  VARCHAR(64) NOT NULL,
    preset     VARCHAR(16) NOT NULL,
    payload    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_analysis_results PRIMARY KEY (page_hash, preset),
    CONSTRAINT ck_analysis_results_preset CHECK (preset IN ('western', 'manga'))
);
