-- V6: database table for stored refresh tokens
-- Matches entity: com.gobro_backend.auth.entity.RefreshToken

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(500) NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);
