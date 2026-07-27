CREATE DATABASE IF NOT EXISTS sabbpeonboarding
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE sabbpeonboarding;

CREATE TABLE IF NOT EXISTS users (
    id             CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    full_name      VARCHAR(255) NOT NULL DEFAULT '',
    mobile_number  VARCHAR(20)  DEFAULT NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS app_role (
    id   VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO app_role (id, name) VALUES
    ('admin', 'Admin'), ('support', 'Support'), ('super_admin', 'Super Admin');

CREATE TABLE IF NOT EXISTS user_roles (
    id         CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    user_id    CHAR(36)     NOT NULL,
    role_id    VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES app_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_profiles (
    id                    CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    user_id               CHAR(36)      NOT NULL UNIQUE,
    full_name             VARCHAR(255)  NOT NULL DEFAULT '',
    mobile_number         VARCHAR(20)   NOT NULL DEFAULT '',
    email                 VARCHAR(255)  NOT NULL DEFAULT '',
    pan_number            VARCHAR(20)   DEFAULT NULL,
    aadhaar_number        VARCHAR(20)   DEFAULT NULL,
    business_name         VARCHAR(255)  DEFAULT NULL,
    gst_number            VARCHAR(50)   DEFAULT NULL,
    entity_type           VARCHAR(50)   DEFAULT 'proprietorship',
    onboarding_status     VARCHAR(30)   NOT NULL DEFAULT 'draft',
    onboarding_score      DOUBLE        DEFAULT NULL,
    rejection_reason      TEXT          DEFAULT NULL,
    reviewed_by           CHAR(36)      DEFAULT NULL,
    reviewed_at           TIMESTAMP     DEFAULT NULL,
    cpv_status            VARCHAR(30)   DEFAULT NULL,
    cpv_video_path        TEXT          DEFAULT NULL,
    cpv_submitted         BOOLEAN       NOT NULL DEFAULT FALSE,
    cpv_submitted_at      TIMESTAMP     DEFAULT NULL,
    cpv_verified_at       TIMESTAMP     DEFAULT NULL,
    cpv_verified_by       CHAR(36)      DEFAULT NULL,
    cpv_rejection_reason  TEXT          DEFAULT NULL,
    risk_level            VARCHAR(20)   DEFAULT NULL,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mp_user (user_id),
    INDEX idx_mp_status (onboarding_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_documents (
    id               CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    merchant_id      CHAR(36)     NOT NULL,
    document_type    VARCHAR(50)  NOT NULL,
    file_name        VARCHAR(255) NOT NULL,
    file_path        TEXT         NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'pending',
    rejection_reason TEXT         DEFAULT NULL,
    uploaded_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at      TIMESTAMP    DEFAULT NULL,
    verified_by      CHAR(36)     DEFAULT NULL,
    INDEX idx_md_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_kyc (
    id                    CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    merchant_id           CHAR(36)     NOT NULL UNIQUE,
    video_kyc_completed   BOOLEAN      NOT NULL DEFAULT FALSE,
    location_captured     BOOLEAN      NOT NULL DEFAULT FALSE,
    latitude              DECIMAL(10,8) DEFAULT NULL,
    longitude             DECIMAL(11,8) DEFAULT NULL,
    video_kyc_file_path   TEXT         DEFAULT NULL,
    selfie_file_path      TEXT         DEFAULT NULL,
    kyc_status            VARCHAR(30)  NOT NULL DEFAULT 'pending',
    verified_at           TIMESTAMP    DEFAULT NULL,
    verified_by           CHAR(36)     DEFAULT NULL,
    rejection_reason      TEXT         DEFAULT NULL,
    full_address          TEXT         DEFAULT NULL,
    city                  VARCHAR(100) DEFAULT NULL,
    state                 VARCHAR(100) DEFAULT NULL,
    pincode               VARCHAR(20)  DEFAULT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_bank_details (
    id                  CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    merchant_id         CHAR(36)     NOT NULL UNIQUE,
    account_number      VARCHAR(50)  NOT NULL,
    ifsc_code           VARCHAR(20)  NOT NULL,
    bank_name           VARCHAR(255) NOT NULL,
    account_holder_name VARCHAR(255) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_persons (
    id                    CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    merchant_id           CHAR(36)     NOT NULL,
    role                  VARCHAR(50)  NOT NULL,
    full_name             VARCHAR(255) NOT NULL,
    pan_number            VARCHAR(20)  DEFAULT NULL,
    is_authorized_signatory BOOLEAN    NOT NULL DEFAULT FALSE,
    sequence_order        INT          DEFAULT 0,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mper_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tickets (
    id              CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    module          VARCHAR(50)  NOT NULL DEFAULT 'merchant_onboarding',
    reference_id    VARCHAR(255) DEFAULT NULL,
    title           VARCHAR(500) NOT NULL,
    description     TEXT         NOT NULL,
    priority        VARCHAR(20)  NOT NULL DEFAULT 'medium',
    status          VARCHAR(30)  NOT NULL DEFAULT 'open',
    created_by      CHAR(36)     NOT NULL,
    assigned_to     CHAR(36)     DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tickets_status (status),
    INDEX idx_tickets_created_by (created_by),
    INDEX idx_tickets_assigned_to (assigned_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ticket_messages (
    id              CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    ticket_id       CHAR(36)     NOT NULL,
    sender_id       CHAR(36)     NOT NULL,
    sender_role     VARCHAR(30)  NOT NULL DEFAULT 'support',
    message         TEXT         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tm_ticket (ticket_id),
    CONSTRAINT fk_tm_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_validations (
    id              CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    document_id     CHAR(36)     NOT NULL,
    merchant_id     CHAR(36)     NOT NULL,
    document_type   VARCHAR(50)  NOT NULL,
    validation_type VARCHAR(50)  NOT NULL,
    is_valid        BOOLEAN      NOT NULL DEFAULT FALSE,
    extracted_data  JSON         DEFAULT NULL,
    validation_notes TEXT        DEFAULT NULL,
    score           INT          DEFAULT 0,
    validated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_credit_checks (
    id              CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    merchant_id     CHAR(36)     NOT NULL,
    provider        VARCHAR(50)  NOT NULL DEFAULT 'experian',
    pan_number      VARCHAR(20)  DEFAULT NULL,
    mobile_number   VARCHAR(20)  DEFAULT NULL,
    credit_score    INT          DEFAULT NULL,
    report_data     JSON         DEFAULT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'pending',
    error_message   TEXT         DEFAULT NULL,
    checked_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP    DEFAULT NULL,
    INDEX idx_mcc_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_kyc_actions (
    id                  CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    support_staff_id    CHAR(36)     NOT NULL,
    merchant_id         CHAR(36)     NOT NULL,
    action              VARCHAR(50)  NOT NULL,
    decision            VARCHAR(50)  NOT NULL,
    notes               TEXT         DEFAULT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
