-- V4: Distributor, Employee, Chat, and other remaining tables

CREATE TABLE IF NOT EXISTS distributor_profiles (
    id                         CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    user_id                    CHAR(36)      NOT NULL UNIQUE,
    company_name               VARCHAR(255)  NOT NULL DEFAULT '',
    contact_person             VARCHAR(255)  NOT NULL DEFAULT '',
    email                      VARCHAR(255)  NOT NULL DEFAULT '',
    mobile_number              VARCHAR(20)   NOT NULL DEFAULT '',
    territory                  VARCHAR(255)  DEFAULT NULL,
    is_active                  BOOLEAN       NOT NULL DEFAULT TRUE,
    address                    TEXT          DEFAULT NULL,
    city                       VARCHAR(100)  DEFAULT NULL,
    state                      VARCHAR(100)  DEFAULT NULL,
    pincode                    VARCHAR(20)   DEFAULT NULL,

    -- Bank
    bank_account_holder        VARCHAR(255)  DEFAULT NULL,
    bank_name                  VARCHAR(255)  DEFAULT NULL,
    bank_account_number        VARCHAR(50)   DEFAULT NULL,
    bank_ifsc                  VARCHAR(20)   DEFAULT NULL,

    -- KYC
    pan_number                 VARCHAR(20)   DEFAULT NULL,
    aadhaar_last4              VARCHAR(4)    DEFAULT NULL,
    pan_document_path          TEXT          DEFAULT NULL,
    aadhaar_document_path      TEXT          DEFAULT NULL,
    profile_photo_path         TEXT          DEFAULT NULL,
    pan_verified               BOOLEAN       NOT NULL DEFAULT FALSE,
    aadhaar_verified           BOOLEAN       NOT NULL DEFAULT FALSE,
    bank_verified              BOOLEAN       NOT NULL DEFAULT FALSE,
    kyc_updated_at             TIMESTAMP     DEFAULT NULL,
    bank_updated_at            TIMESTAMP     DEFAULT NULL,

    -- Commercials
    default_commission_rate    DECIMAL(5,2)  DEFAULT NULL,
    payout_cycle               VARCHAR(20)   DEFAULT 'daily',
    security_deposit           DECIMAL(12,2) NOT NULL DEFAULT 0,

    -- Recovery
    available_recovery_balance DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_recovered_amount     DECIMAL(12,2) NOT NULL DEFAULT 0,

    -- Agreement / Onboarding
    agreement_status           VARCHAR(30)   NOT NULL DEFAULT 'pending',
    agreement_file_path        TEXT          DEFAULT NULL,
    agreement_sent_at          TIMESTAMP     DEFAULT NULL,
    agreement_sent_by          CHAR(36)      DEFAULT NULL,
    signed_agreement_path      TEXT          DEFAULT NULL,
    agreement_uploaded_at      TIMESTAMP     DEFAULT NULL,
    agreement_approved_at      TIMESTAMP     DEFAULT NULL,
    agreement_approved_by      CHAR(36)      DEFAULT NULL,
    agreement_rejection_reason TEXT          DEFAULT NULL,
    credentials_sent_at        TIMESTAMP     DEFAULT NULL,
    credentials_sent_by        CHAR(36)      DEFAULT NULL,
    onboarding_completed_at    TIMESTAMP     DEFAULT NULL,
    onboarding_token           VARCHAR(255)  DEFAULT NULL,
    onboarding_token_expires_at TIMESTAMP    DEFAULT NULL,
    onboarding_token_used_at   TIMESTAMP     DEFAULT NULL,
    kyc_status                 VARCHAR(30)   NOT NULL DEFAULT 'pending',
    kyc_submitted_at           TIMESTAMP     DEFAULT NULL,
    kyc_verified_at            TIMESTAMP     DEFAULT NULL,
    kyc_verified_by            CHAR(36)      DEFAULT NULL,

    created_at                 TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_dp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_dp_user (user_id),
    INDEX idx_dp_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS employee_profiles (
    id            CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    user_id       CHAR(36)     NOT NULL UNIQUE,
    full_name     VARCHAR(255) NOT NULL DEFAULT '',
    mobile_number VARCHAR(20)  NOT NULL DEFAULT '',
    email         VARCHAR(255) NOT NULL UNIQUE,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ep_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_ep_user (user_id),
    INDEX idx_ep_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_audio_logs (
    id                 CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    user_id            CHAR(36)     DEFAULT NULL,
    session_id         VARCHAR(255) NOT NULL,
    audio_storage_path TEXT         DEFAULT NULL,
    transcript         TEXT         NOT NULL DEFAULT '',
    language           VARCHAR(10)  NOT NULL DEFAULT 'en',
    onboarding_step    VARCHAR(100) DEFAULT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cal_user (user_id),
    INDEX idx_cal_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
