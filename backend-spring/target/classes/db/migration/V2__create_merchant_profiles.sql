-- V2: Merchant onboarding tables

CREATE TABLE IF NOT EXISTS merchant_profiles (
    id                                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    user_id                           CHAR(36)      NOT NULL UNIQUE,
    full_name                         VARCHAR(255)  NOT NULL DEFAULT '',
    mobile_number                     VARCHAR(20)   NOT NULL DEFAULT '',
    email                             VARCHAR(255)  NOT NULL DEFAULT '',
    pan_number                        VARCHAR(20)   DEFAULT NULL,
    aadhaar_number                    VARCHAR(20)   DEFAULT NULL,
    business_name                     VARCHAR(255)  DEFAULT NULL,
    gst_number                        VARCHAR(50)   DEFAULT NULL,
    entity_type                       VARCHAR(50)   DEFAULT 'proprietorship',
    onboarding_status                 VARCHAR(30)   NOT NULL DEFAULT 'pending',
    distributor_id                    CHAR(36)      DEFAULT NULL,
    commission                        DECIMAL(5,2)  DEFAULT NULL,

    -- Business address
    business_address_line1            VARCHAR(255)  DEFAULT NULL,
    business_address_line2            VARCHAR(255)  DEFAULT NULL,
    business_city                     VARCHAR(100)  DEFAULT NULL,
    business_state                    VARCHAR(100)  DEFAULT NULL,
    business_postal_code              VARCHAR(20)   DEFAULT NULL,
    business_country                  VARCHAR(100)  DEFAULT 'India',

    -- Selected products & costs
    selected_products                 JSON          DEFAULT NULL,
    total_monthly_cost                DECIMAL(12,2) DEFAULT 0,
    total_onetime_cost                DECIMAL(12,2) DEFAULT 0,
    total_integration_cost            DECIMAL(12,2) DEFAULT 0,

    -- Agreement
    agreement_signed                  BOOLEAN       NOT NULL DEFAULT FALSE,
    agreement_signed_at               TIMESTAMP     DEFAULT NULL,
    agreement_ip_address              VARCHAR(45)   DEFAULT NULL,
    agreement_signature               TEXT          DEFAULT NULL,

    -- Settlement config
    rolling_reserve_enabled           BOOLEAN       NOT NULL DEFAULT FALSE,
    rolling_reserve_percentage        DECIMAL(5,2)  DEFAULT NULL,
    rolling_reserve_fixed_inr         DECIMAL(12,2) DEFAULT NULL,
    settlement_cycle_days             SMALLINT      NOT NULL DEFAULT 1,
    settlement_terms_locked           BOOLEAN       NOT NULL DEFAULT FALSE,
    settlement_config_overridden_by_admin BOOLEAN   NOT NULL DEFAULT FALSE,
    settlement_config_overridden_at   TIMESTAMP     DEFAULT NULL,
    settlement_config_overridden_by   CHAR(36)      DEFAULT NULL,
    settlement_config_override_reason TEXT          DEFAULT NULL,
    undertaking_pdf_url               TEXT          DEFAULT NULL,

    -- Balance
    pending_settlement_amount         DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_settled_amount              DECIMAL(12,2) NOT NULL DEFAULT 0,
    last_settled_at                   TIMESTAMP     DEFAULT NULL,
    total_chargeback_amount           DECIMAL(12,2) NOT NULL DEFAULT 0,
    pending_chargeback_amount         DECIMAL(12,2) NOT NULL DEFAULT 0,
    chargeback_recovery_available     DECIMAL(12,2) NOT NULL DEFAULT 0,

    -- Bank application tracking
    bank_application_id               VARCHAR(100)  DEFAULT NULL,
    bank_merchant_code                VARCHAR(100)  DEFAULT NULL,

    -- Split / PG config
    split_settlement_enabled          BOOLEAN       NOT NULL DEFAULT FALSE,
    split_percentage                  DECIMAL(5,2)  DEFAULT NULL,
    split_account_number              VARCHAR(50)   DEFAULT NULL,
    split_ifsc_code                   VARCHAR(20)   DEFAULT NULL,
    pg_commercials_accepted           BOOLEAN       NOT NULL DEFAULT FALSE,
    pg_agreement_signed               BOOLEAN       NOT NULL DEFAULT FALSE,
    cpv_submitted                     BOOLEAN       NOT NULL DEFAULT FALSE,

    -- Transaction Id (from payment system)
    transaction_id                    VARCHAR(255)  DEFAULT NULL,
    txn_details                       JSON          DEFAULT NULL,
    invitation_token                  VARCHAR(255)  DEFAULT NULL,
    invited_via                       VARCHAR(50)   DEFAULT NULL,

    -- Rejection
    rejection_reason                  TEXT          DEFAULT NULL,
    reviewed_at                       TIMESTAMP     DEFAULT NULL,
    reviewed_by                       CHAR(36)      DEFAULT NULL,
    risk_level                        VARCHAR(20)   DEFAULT NULL,

    -- Timestamps
    created_at                        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_mp_user (user_id),
    INDEX idx_mp_distributor (distributor_id),
    INDEX idx_mp_status (onboarding_status),
    INDEX idx_mp_settlement_locked (settlement_terms_locked),
    INDEX idx_mp_bank_app (bank_application_id),
    INDEX idx_mp_transaction (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_bank_details (
    id                  CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    merchant_id         CHAR(36)     NOT NULL UNIQUE,
    account_number      VARCHAR(50)  NOT NULL,
    ifsc_code           VARCHAR(20)  NOT NULL,
    bank_name           VARCHAR(255) NOT NULL,
    account_holder_name VARCHAR(255) NOT NULL,
    upi_vpa             VARCHAR(100) DEFAULT NULL,
    upi_qr_string       TEXT         DEFAULT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mbd_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_documents (
    id               CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    merchant_id      CHAR(36)     NOT NULL,
    document_type    VARCHAR(50)  NOT NULL,
    file_name        VARCHAR(255) NOT NULL,
    file_path        TEXT         NOT NULL,
    file_size        BIGINT       DEFAULT NULL,
    mime_type        VARCHAR(100) DEFAULT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'pending',
    rejection_reason TEXT         DEFAULT NULL,
    uploaded_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at      TIMESTAMP    DEFAULT NULL,
    verified_by      CHAR(36)     DEFAULT NULL,
    doc_category     VARCHAR(50)  DEFAULT NULL,
    person_id        CHAR(36)     DEFAULT NULL,
    CONSTRAINT fk_md_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    INDEX idx_md_merchant (merchant_id),
    INDEX idx_md_type (document_type)
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
    completed_at          TIMESTAMP    DEFAULT NULL,
    verified_at           TIMESTAMP    DEFAULT NULL,
    verified_by           CHAR(36)     DEFAULT NULL,
    rejection_reason      TEXT         DEFAULT NULL,
    full_address          TEXT         DEFAULT NULL,
    area                  VARCHAR(255) DEFAULT NULL,
    city                  VARCHAR(100) DEFAULT NULL,
    state                 VARCHAR(100) DEFAULT NULL,
    pincode               VARCHAR(20)  DEFAULT NULL,
    country               VARCHAR(100) DEFAULT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mk_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS onboarding_audit_log (
    id              CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    merchant_id     CHAR(36)     NOT NULL,
    action          VARCHAR(255) NOT NULL,
    previous_status VARCHAR(50)  DEFAULT NULL,
    new_status      VARCHAR(50)  DEFAULT NULL,
    performed_by    CHAR(36)     DEFAULT NULL,
    notes           TEXT         DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_oal_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    INDEX idx_oal_merchant (merchant_id),
    INDEX idx_oal_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_persons (
    id                    CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    merchant_id           CHAR(36)     NOT NULL,
    role                  VARCHAR(50)  NOT NULL,
    full_name             VARCHAR(255) NOT NULL,
    pan_number            VARCHAR(20)  DEFAULT NULL,
    address_proof_type    VARCHAR(50)  DEFAULT NULL,
    is_authorized_signatory BOOLEAN    NOT NULL DEFAULT FALSE,
    sequence_order        INT          DEFAULT 0,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mp_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    INDEX idx_mp_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_invitations (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    merchant_name     VARCHAR(255)  NOT NULL,
    merchant_mobile   VARCHAR(20)   NOT NULL,
    invitation_token  VARCHAR(255)  NOT NULL,
    distributor_id    CHAR(36)      DEFAULT NULL,
    status            VARCHAR(30)   DEFAULT 'pending',
    sent_at           TIMESTAMP     DEFAULT NULL,
    sent_via          VARCHAR(20)   DEFAULT NULL,
    expires_at        TIMESTAMP     DEFAULT NULL,
    accepted_at       TIMESTAMP     DEFAULT NULL,
    metadata          JSON          DEFAULT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mi_token (invitation_token),
    INDEX idx_mi_distributor (distributor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
