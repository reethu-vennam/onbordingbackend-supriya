-- =====================================================
-- SabbPe Merchant Onboarding - MariaDB Schema
-- Database: sabbpeonboarding
-- =====================================================

CREATE DATABASE IF NOT EXISTS sabbpeonboarding
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE sabbpeonboarding;

-- =====================================================
-- V1: Users, Roles, and Auth tables
-- =====================================================

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

CREATE TABLE IF NOT EXISTS app_role (
    id   VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO app_role (id, name) VALUES
    ('admin',        'Admin'),
    ('moderator',    'Moderator'),
    ('user',         'User'),
    ('distributor',  'Distributor'),
    ('merchant',     'Merchant'),
    ('bank_staff',   'Bank Staff'),
    ('support_staff','Support Staff'),
    ('employee',     'Employee');

CREATE TABLE IF NOT EXISTS user_roles (
    id         CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    user_id    CHAR(36)     NOT NULL,
    role_id    VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_ur_user (user_id),
    INDEX idx_ur_role (role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES app_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    user_id     CHAR(36)      NOT NULL,
    token       VARCHAR(512)  NOT NULL UNIQUE,
    expires_at  TIMESTAMP     NOT NULL,
    revoked     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rt_token (token),
    INDEX idx_rt_user (user_id),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- V2: Merchant onboarding tables
-- =====================================================

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
    onboarding_status                 VARCHAR(30)   NOT NULL DEFAULT 'draft',
    distributor_id                    CHAR(36)      DEFAULT NULL,
    commission                        DECIMAL(5,2)  DEFAULT NULL,
    business_address_line1            VARCHAR(255)  DEFAULT NULL,
    business_address_line2            VARCHAR(255)  DEFAULT NULL,
    business_city                     VARCHAR(100)  DEFAULT NULL,
    business_state                    VARCHAR(100)  DEFAULT NULL,
    business_postal_code              VARCHAR(20)   DEFAULT NULL,
    business_country                  VARCHAR(100)  DEFAULT 'India',
    selected_products                 JSON          DEFAULT NULL,
    total_monthly_cost                DECIMAL(12,2) DEFAULT 0.00,
    total_onetime_cost                DECIMAL(12,2) DEFAULT 0.00,
    total_integration_cost            DECIMAL(12,2) DEFAULT 0.00,
    agreement_signed                  BOOLEAN       NOT NULL DEFAULT FALSE,
    agreement_signed_at               TIMESTAMP     DEFAULT NULL,
    agreement_ip_address              VARCHAR(45)   DEFAULT NULL,
    agreement_signature               TEXT          DEFAULT NULL,
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
    pending_settlement_amount         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_settled_amount              DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    last_settled_at                   TIMESTAMP     DEFAULT NULL,
    total_chargeback_amount           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    pending_chargeback_amount         DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    chargeback_recovery_available     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    bank_application_id               VARCHAR(100)  DEFAULT NULL,
    bank_merchant_code                VARCHAR(100)  DEFAULT NULL,
    split_settlement_enabled          BOOLEAN       NOT NULL DEFAULT FALSE,
    split_percentage                  DECIMAL(5,2)  DEFAULT NULL,
    split_account_number              VARCHAR(50)   DEFAULT NULL,
    split_ifsc_code                   VARCHAR(20)   DEFAULT NULL,
    pg_commercials_accepted           BOOLEAN       NOT NULL DEFAULT FALSE,
    pg_agreement_signed               BOOLEAN       NOT NULL DEFAULT FALSE,
    cpv_submitted                     BOOLEAN       NOT NULL DEFAULT FALSE,
    transaction_id                    VARCHAR(255)  DEFAULT NULL,
    txn_details                       JSON          DEFAULT NULL,
    invitation_token                  VARCHAR(255)  DEFAULT NULL,
    invited_via                       VARCHAR(50)   DEFAULT NULL,
    rejection_reason                  TEXT          DEFAULT NULL,
    reviewed_at                       TIMESTAMP     DEFAULT NULL,
    reviewed_by                       CHAR(36)      DEFAULT NULL,
    risk_level                        VARCHAR(20)   DEFAULT NULL,
    created_at                        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mp_user (user_id),
    INDEX idx_mp_distributor (distributor_id),
    INDEX idx_mp_status (onboarding_status),
    INDEX idx_mp_bank_app (bank_application_id),
    INDEX idx_mp_transaction (transaction_id),
    CONSTRAINT fk_mp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
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
    INDEX idx_md_merchant (merchant_id),
    INDEX idx_md_type (document_type),
    CONSTRAINT fk_md_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
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
    INDEX idx_oal_merchant (merchant_id),
    INDEX idx_oal_created (created_at DESC),
    CONSTRAINT fk_oal_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
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
    INDEX idx_mper_merchant (merchant_id),
    CONSTRAINT fk_mper_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
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

-- =====================================================
-- V3: Products catalog, agreements, transactions
-- =====================================================

CREATE TABLE IF NOT EXISTS product_catalog (
    id                  CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    product_code        VARCHAR(50)   NOT NULL UNIQUE,
    product_name        VARCHAR(255)  NOT NULL,
    product_description TEXT          DEFAULT NULL,
    features            JSON          DEFAULT NULL,
    price               DECIMAL(12,2) DEFAULT 0.00,
    price_type          VARCHAR(30)   DEFAULT NULL,
    price_monthly_min   DECIMAL(12,2) DEFAULT NULL,
    price_monthly_max   DECIMAL(12,2) DEFAULT NULL,
    price_onetime_min   DECIMAL(12,2) DEFAULT NULL,
    price_onetime_max   DECIMAL(12,2) DEFAULT NULL,
    price_integration_fee DECIMAL(12,2) DEFAULT NULL,
    price_amc           DECIMAL(12,2) DEFAULT NULL,
    price_mid           DECIMAL(12,2) DEFAULT NULL,
    price_sim_cost_min  DECIMAL(12,2) DEFAULT NULL,
    price_sim_cost_max  DECIMAL(12,2) DEFAULT NULL,
    display_price       DECIMAL(12,2) DEFAULT NULL,
    display_price_type  VARCHAR(30)   DEFAULT NULL,
    pricing_note        TEXT          DEFAULT NULL,
    product_image_url   TEXT          DEFAULT NULL,
    category            VARCHAR(50)   NOT NULL DEFAULT 'software',
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    display_order       INT           DEFAULT 0,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pc_code (product_code),
    INDEX idx_pc_category (category),
    INDEX idx_pc_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_sub_catalog (
    id                  CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    parent_product_code VARCHAR(50)   NOT NULL,
    product_code        VARCHAR(50)   NOT NULL,
    product_name        VARCHAR(255)  NOT NULL,
    product_description TEXT          DEFAULT NULL,
    price               DECIMAL(12,2) DEFAULT 0.00,
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    display_order       INT           DEFAULT 0,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_psc_parent (parent_product_code),
    INDEX idx_psc_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_sub_products (
    id                  CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    merchant_profile_id CHAR(36)     NOT NULL,
    parent_product_code VARCHAR(50)  NOT NULL,
    sub_product_code    VARCHAR(50)  NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msp_sub (merchant_profile_id, sub_product_code),
    INDEX idx_msp_profile (merchant_profile_id),
    CONSTRAINT fk_msp_profile FOREIGN KEY (merchant_profile_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_agreements (
    id                    CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    merchant_id           CHAR(36)      NOT NULL,
    agreement_type        VARCHAR(50)   NOT NULL,
    agreement_version     VARCHAR(20)   DEFAULT NULL,
    selected_products     JSON          DEFAULT NULL,
    total_monthly_cost    DECIMAL(12,2) DEFAULT 0.00,
    total_onetime_cost    DECIMAL(12,2) DEFAULT 0.00,
    total_integration_cost DECIMAL(12,2) DEFAULT 0.00,
    agreement_text        LONGTEXT      DEFAULT NULL,
    terms_html            LONGTEXT      DEFAULT NULL,
    signed                BOOLEAN       NOT NULL DEFAULT FALSE,
    signed_at             TIMESTAMP     DEFAULT NULL,
    signature_name        VARCHAR(255)  DEFAULT NULL,
    ip_address            VARCHAR(45)   DEFAULT NULL,
    user_agent            TEXT          DEFAULT NULL,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ma_merchant (merchant_id),
    CONSTRAINT fk_ma_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS transactions (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    merchant_id       CHAR(36)      NOT NULL,
    transaction_id    VARCHAR(255)  NOT NULL UNIQUE,
    amount            DECIMAL(12,2) NOT NULL,
    currency          VARCHAR(10)   NOT NULL DEFAULT 'INR',
    status            VARCHAR(30)   NOT NULL,
    payment_method    VARCHAR(50)   DEFAULT NULL,
    customer_name     VARCHAR(255)  DEFAULT NULL,
    customer_email    VARCHAR(255)  DEFAULT NULL,
    customer_mobile   VARCHAR(20)   DEFAULT NULL,
    metadata          JSON          DEFAULT NULL,
    settlement_status VARCHAR(20)   NOT NULL DEFAULT 'unsettled',
    settlement_batch_id CHAR(36)    DEFAULT NULL,
    settled_at        TIMESTAMP     DEFAULT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_txn_merchant (merchant_id),
    INDEX idx_txn_status (status),
    INDEX idx_txn_created (created_at DESC),
    INDEX idx_txn_id (transaction_id),
    INDEX idx_txn_settlement_status (settlement_status),
    CONSTRAINT fk_txn_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications (
    id          CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    user_id     CHAR(36)      DEFAULT NULL,
    title       VARCHAR(255)  NOT NULL,
    message     TEXT          NOT NULL,
    type        VARCHAR(50)   NOT NULL,
    is_read     BOOLEAN       NOT NULL DEFAULT FALSE,
    read_at     TIMESTAMP     DEFAULT NULL,
    action_label VARCHAR(255) DEFAULT NULL,
    action_url  TEXT          DEFAULT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notif_user (user_id),
    INDEX idx_notif_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS application_status_history (
    id              CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    merchant_id     CHAR(36)     DEFAULT NULL,
    previous_status VARCHAR(50)  DEFAULT NULL,
    new_status      VARCHAR(50)  DEFAULT NULL,
    reason          TEXT         DEFAULT NULL,
    changed_by      VARCHAR(255) DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ash_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- V4: Distributor, Employee, Chat tables
-- =====================================================

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
    bank_account_holder        VARCHAR(255)  DEFAULT NULL,
    bank_name                  VARCHAR(255)  DEFAULT NULL,
    bank_account_number        VARCHAR(50)   DEFAULT NULL,
    bank_ifsc                  VARCHAR(20)   DEFAULT NULL,
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
    default_commission_rate    DECIMAL(5,2)  DEFAULT NULL,
    payout_cycle               VARCHAR(20)   DEFAULT 'daily',
    security_deposit           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    available_recovery_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_recovered_amount     DECIMAL(12,2) NOT NULL DEFAULT 0.00,
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
    INDEX idx_dp_user (user_id),
    INDEX idx_dp_email (email),
    CONSTRAINT fk_dp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
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
    INDEX idx_ep_user (user_id),
    INDEX idx_ep_email (email),
    CONSTRAINT fk_ep_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
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

-- =====================================================
-- V5: Settlement engine, rolling reserve, chargebacks
-- =====================================================

CREATE TABLE IF NOT EXISTS settlement_history (
    id                   CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    merchant_id          CHAR(36)      NOT NULL,
    distributor_id       CHAR(36)      NOT NULL,
    settlement_batch_ref VARCHAR(100)  NOT NULL,
    settlement_date      DATE          NOT NULL DEFAULT (CURRENT_DATE),
    settlement_cycle_days SMALLINT     NOT NULL,
    gross_amount         DECIMAL(12,2) NOT NULL,
    mdr_deduction        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    rolling_reserve_held DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    net_settlement_amount DECIMAL(12,2) NOT NULL,
    transaction_count    INT           NOT NULL,
    transaction_refs     JSON          NOT NULL DEFAULT '[]',
    status               VARCHAR(20)   NOT NULL DEFAULT 'pending',
    processed_at         TIMESTAMP     DEFAULT NULL,
    failure_reason       TEXT          DEFAULT NULL,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sh_batch (merchant_id, settlement_batch_ref),
    INDEX idx_sh_merchant (merchant_id),
    INDEX idx_sh_distributor (distributor_id),
    INDEX idx_sh_date (settlement_date DESC),
    INDEX idx_sh_batch_ref (settlement_batch_ref),
    INDEX idx_sh_status (status),
    CONSTRAINT fk_sh_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rolling_reserve_ledger (
    id                    CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    merchant_id           CHAR(36)      NOT NULL,
    distributor_id        CHAR(36)      NOT NULL,
    transaction_ref       VARCHAR(255)  NOT NULL,
    gross_settlement_amount DECIMAL(12,2) NOT NULL,
    reserve_amount        DECIMAL(12,2) NOT NULL,
    reserve_date          DATE          NOT NULL DEFAULT (CURRENT_DATE),
    release_date          DATE          NOT NULL,
    status                VARCHAR(20)   NOT NULL DEFAULT 'held',
    debit_reason          TEXT          DEFAULT NULL,
    settlement_cycle_days SMALLINT      NOT NULL,
    released_at           TIMESTAMP     DEFAULT NULL,
    debited_at            TIMESTAMP     DEFAULT NULL,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rrl_batch (merchant_id, transaction_ref),
    INDEX idx_rrl_merchant (merchant_id),
    INDEX idx_rrl_distributor (distributor_id),
    INDEX idx_rrl_status (status),
    INDEX idx_rrl_release (release_date),
    INDEX idx_rrl_merchant_status (merchant_id, status),
    CONSTRAINT fk_rrl_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chargebacks (
    id                  CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    merchant_id         CHAR(36)      NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    currency            VARCHAR(10)   NOT NULL DEFAULT 'INR',
    reason              TEXT          NOT NULL,
    status              VARCHAR(30)   NOT NULL DEFAULT 'pending',
    chargeback_date     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recovered_at        TIMESTAMP     DEFAULT NULL,
    recovery_source     VARCHAR(50)   DEFAULT NULL,
    recovery_steps      JSON          NOT NULL DEFAULT '[]',
    metadata            JSON          DEFAULT '{}',
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cb_merchant (merchant_id),
    INDEX idx_cb_status (status),
    INDEX idx_cb_date (chargeback_date DESC),
    CONSTRAINT fk_cb_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chargeback_history (
    id                CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    chargeback_id     CHAR(36)      NOT NULL,
    merchant_id       CHAR(36)      NOT NULL,
    action            VARCHAR(50)   NOT NULL,
    event_type        VARCHAR(50)   NOT NULL,
    previous_data     JSON          DEFAULT NULL,
    current_data      JSON          DEFAULT NULL,
    recovered_amount  DECIMAL(12,2) DEFAULT NULL,
    recovery_source   VARCHAR(50)   DEFAULT NULL,
    recovery_details  JSON          DEFAULT NULL,
    event_timestamp   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    performed_by      VARCHAR(255)  NOT NULL,
    comments          TEXT          DEFAULT NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cbh_chargeback (chargeback_id),
    INDEX idx_cbh_merchant (merchant_id),
    INDEX idx_cbh_event (event_type),
    INDEX idx_cbh_timestamp (event_timestamp DESC),
    CONSTRAINT fk_cbh_chargeback FOREIGN KEY (chargeback_id) REFERENCES chargebacks(id) ON DELETE CASCADE,
    CONSTRAINT fk_cbh_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS distributor_recovery_history (
    id                  CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    distributor_id      CHAR(36)      NOT NULL,
    chargeback_id       CHAR(36)      NOT NULL,
    merchant_id         CHAR(36)      NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    recovery_source     VARCHAR(50)   NOT NULL DEFAULT 'distributor_balance',
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_drh_chargeback (distributor_id, chargeback_id),
    INDEX idx_drh_distributor (distributor_id),
    INDEX idx_drh_chargeback (chargeback_id),
    INDEX idx_drh_created (created_at DESC),
    CONSTRAINT fk_drh_distributor FOREIGN KEY (distributor_id) REFERENCES distributor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_drh_chargeback FOREIGN KEY (chargeback_id) REFERENCES chargebacks(id) ON DELETE CASCADE,
    CONSTRAINT fk_drh_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Stored Procedures
-- =====================================================

DELIMITER //

CREATE OR REPLACE PROCEDURE increment_merchant_balances(
    IN p_merchant_id CHAR(36),
    IN p_pending_increment DECIMAL(12,2),
    IN p_total_increment DECIMAL(12,2)
)
BEGIN
    UPDATE merchant_profiles
    SET
        pending_settlement_amount = pending_settlement_amount + COALESCE(p_pending_increment, 0),
        total_settled_amount = total_settled_amount + COALESCE(p_total_increment, 0),
        last_settled_at = CURRENT_TIMESTAMP
    WHERE id = p_merchant_id;
END //

CREATE OR REPLACE PROCEDURE update_chargeback_balances(
    IN p_merchant_id CHAR(36),
    IN p_chargeback_amount DECIMAL(12,2),
    IN p_recovered_amount DECIMAL(12,2)
)
BEGIN
    UPDATE merchant_profiles
    SET
        total_chargeback_amount = total_chargeback_amount + COALESCE(p_chargeback_amount, 0),
        pending_chargeback_amount = pending_chargeback_amount + COALESCE(p_chargeback_amount, 0),
        chargeback_recovery_available = chargeback_recovery_available + COALESCE(p_recovered_amount, 0)
    WHERE id = p_merchant_id;
END //


DELIMITER ;

-- =====================================================
-- Schema complete! 22 tables + 2 stored procedures
-- =====================================================