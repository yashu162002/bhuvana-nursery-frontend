-- Persistent administrator credentials.
-- Previously the admin user lived only in memory (seeded from ADMIN_USERNAME /
-- ADMIN_PASSWORD at startup), which made a password change impossible to
-- persist. The first row is still seeded from those environment variables by
-- AdminUserInitializer when the table is empty; after that the database is the
-- source of truth, so a password reset survives restarts and deployments.
CREATE TABLE admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    email VARCHAR(320) UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    password_changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Single-use password reset tokens. Only the SHA-256 hash of a token is
-- stored, so a database leak cannot be replayed into account takeovers.
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id UUID NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_token_hash_length CHECK (length(token_hash) = 64)
);

CREATE INDEX idx_password_reset_tokens_user
    ON password_reset_tokens(admin_user_id, created_at DESC);

CREATE INDEX idx_password_reset_tokens_expires
    ON password_reset_tokens(expires_at);
