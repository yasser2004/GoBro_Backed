-- V1: core identity tables (Role, Permission, User)
-- Matches entities: com.gobro_backend.user.entity.{Role, Permission, User}

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE permissions (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    full_name           VARCHAR(100)  NOT NULL,
    email               VARCHAR(150)  NOT NULL,
    password            VARCHAR(255)  NOT NULL,
    avatar_url          VARCHAR(500),
    bio                 VARCHAR(1000),
    phone_number        VARCHAR(30),
    provider            VARCHAR(20)   NOT NULL DEFAULT 'LOCAL',
    enabled             BOOLEAN       NOT NULL DEFAULT TRUE,
    two_factor_enabled  BOOLEAN       NOT NULL DEFAULT FALSE,
    two_factor_secret   VARCHAR(100),
    xp                  INTEGER       NOT NULL DEFAULT 0,
    level               INTEGER       NOT NULL DEFAULT 1,
    role_id             BIGINT        NOT NULL REFERENCES roles(id),
    last_login_at       TIMESTAMP,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(150),
    updated_by          VARCHAR(150)
);

CREATE UNIQUE INDEX idx_users_email ON users (email);

-- Seed the 3 platform roles (cahier des charges §6 - Rôles utilisateurs)
INSERT INTO roles (name, description) VALUES
    ('ETUDIANT', 'Apprenant : achète des formations, suit sa progression, passe les quiz'),
    ('FORMATEUR', 'Créateur de contenu : publie des cours, gère ses étudiants et ses revenus'),
    ('ADMIN', 'Administrateur de la plateforme : validation des cours, gestion des utilisateurs et des paiements');