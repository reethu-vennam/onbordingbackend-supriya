-- V1: Users, Roles, and Auth tables (replaces Supabase auth.users + user_roles)

CREATE TABLE IF NOT EXISTS app_role (
    id   VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO app_role (id, name) VALUES
    ('admin',        'Admin'),
    ('moderator',    'Moderator'),
    ('user',         'User'),
    ('distributor',  'Distributor'),
    ('merchant',     'Merchant'),
    ('bank_staff',   'Bank Staff'),
    ('support_staff','Support Staff'),
    ('employee',     'Employee')
ON DUPLICATE KEY UPDATE name = VALUES(name);

CREATE TABLE IF NOT EXISTS users (
    id             CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(255) NOT NULL DEFAULT '',
    mobile_number  VARCHAR(20)  DEFAULT NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_mobile (mobile_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    id         CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    user_id    CHAR(36)     NOT NULL,
    role_id    VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES app_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    user_id     CHAR(36)     NOT NULL,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_rt_token (token),
    INDEX idx_rt_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
