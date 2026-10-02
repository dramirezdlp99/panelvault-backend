-- V1: cuentas de usuario del modulo identity.
--
-- El id lo genera la aplicacion (UUID aleatorio), no la base.
-- El correo se guarda normalizado en minusculas; el CHECK impide que alguien
-- escriba directo en la tabla saltandose esa regla y duplique cuentas
-- con "Ana@Mail.com" y "ana@mail.com".
-- password_hash guarda el hash BCrypt (60 caracteres); se deja margen por si se
-- cambia de algoritmo.

CREATE TABLE users (
                       id            UUID         NOT NULL,
                       email         VARCHAR(254) NOT NULL,
                       display_name  VARCHAR(40)  NOT NULL,
                       password_hash VARCHAR(100) NOT NULL,
                       role          VARCHAR(20)  NOT NULL,
                       created_at    TIMESTAMPTZ  NOT NULL,
                       updated_at    TIMESTAMPTZ  NOT NULL,
                       CONSTRAINT pk_users PRIMARY KEY (id),
                       CONSTRAINT uq_users_email UNIQUE (email),
                       CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email)),
                       CONSTRAINT ck_users_role CHECK (role IN ('LECTOR', 'CURADOR', 'ADMIN'))
);