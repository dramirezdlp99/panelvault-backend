-- V7: catalogo publico de comics libres (dominio publico o Creative Commons).
--
-- Lo administran los curadores; solo las obras con published = TRUE se ven en
-- las paginas publicas (/explorar). El slug es la parte legible de la URL y no
-- cambia aunque se corrija el titulo. created_by es NULL en las obras sembradas
-- por esta migracion.

CREATE TABLE catalog_works (
    id               UUID          NOT NULL,
    slug             VARCHAR(80)   NOT NULL,
    title            VARCHAR(200)  NOT NULL,
    author           VARCHAR(120)  NOT NULL,
    publication_year INTEGER,
    publisher        VARCHAR(120),
    description      VARCHAR(2000),
    source_url       VARCHAR(500)  NOT NULL,
    cover_url        VARCHAR(500),
    license          VARCHAR(20)   NOT NULL,
    page_count       INTEGER,
    tags             VARCHAR(400)  NOT NULL DEFAULT '',
    published        BOOLEAN       NOT NULL DEFAULT FALSE,
    published_at     TIMESTAMPTZ,
    created_by       UUID,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_catalog_works PRIMARY KEY (id),
    CONSTRAINT uq_catalog_works_slug UNIQUE (slug),
    CONSTRAINT fk_catalog_works_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_catalog_works_slug CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_catalog_works_license CHECK (license IN ('PUBLIC_DOMAIN', 'CC0', 'CC_BY', 'CC_BY_SA')),
    CONSTRAINT ck_catalog_works_source CHECK (source_url LIKE 'https://%'),
    CONSTRAINT ck_catalog_works_published CHECK (published = FALSE OR published_at IS NOT NULL)
);

CREATE INDEX ix_catalog_works_published_title ON catalog_works (title) WHERE published = TRUE;

-- Obras iniciales: tiras clasicas de prensa estadounidense publicadas antes de 1929,
-- hoy de dominio publico en Estados Unidos. Los textos llevan tildes: el archivo esta
-- en UTF-8, que es la codificacion que Flyway usa por defecto.
INSERT INTO catalog_works (id, slug, title, author, publication_year, publisher, description, source_url,
                           license, tags, published, published_at, created_at, updated_at)
VALUES
    ('7c1e3a52-0b8f-4d61-9a4e-2f6b8c1d0a01', 'little-nemo-in-slumberland', 'Little Nemo in Slumberland',
     'Winsor McCay', 1905, 'New York Herald',
     'Cada domingo, el pequeño Nemo vive un sueño extraordinario en el reino de Slumberland y despierta en '
     || 'la última viñeta. Famosa por su arquitectura onírica y su uso innovador del tamaño de las viñetas.',
     'https://en.wikipedia.org/wiki/Little_Nemo', 'PUBLIC_DOMAIN', 'tira de prensa,fantasía,clásicos',
     TRUE, now(), now(), now()),
    ('7c1e3a52-0b8f-4d61-9a4e-2f6b8c1d0a02', 'the-yellow-kid', 'The Yellow Kid',
     'Richard F. Outcault', 1895, 'New York World',
     'El niño del camisón amarillo de Hogan''s Alley, uno de los primeros personajes recurrentes de la '
     || 'historieta de prensa y pionero del uso del texto dentro de la imagen.',
     'https://en.wikipedia.org/wiki/The_Yellow_Kid', 'PUBLIC_DOMAIN', 'tira de prensa,humor,clásicos',
     TRUE, now(), now(), now()),
    ('7c1e3a52-0b8f-4d61-9a4e-2f6b8c1d0a03', 'krazy-kat', 'Krazy Kat',
     'George Herriman', 1913, 'New York Evening Journal',
     'El triángulo entre Krazy, el ratón Ignatz y el oficial Pupp en el desierto de Coconino. Las tiras '
     || 'publicadas antes de 1929 son de dominio público en Estados Unidos.',
     'https://en.wikipedia.org/wiki/Krazy_Kat', 'PUBLIC_DOMAIN', 'tira de prensa,humor,surrealismo',
     TRUE, now(), now(), now());
