-- V3: Products catalog, agreements, transactions

CREATE TABLE IF NOT EXISTS product_catalog (
    id                  CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    product_code        VARCHAR(50)   NOT NULL UNIQUE,
    product_name        VARCHAR(255)  NOT NULL,
    product_description TEXT          DEFAULT NULL,
    features            JSON          DEFAULT NULL,
    price               DECIMAL(12,2) DEFAULT 0,
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
    price               DECIMAL(12,2) DEFAULT 0,
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
    CONSTRAINT fk_msp_profile FOREIGN KEY (merchant_profile_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    INDEX idx_msp_profile (merchant_profile_id),
    UNIQUE KEY uk_msp_sub (merchant_profile_id, sub_product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_agreements (
    id                    CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    merchant_id           CHAR(36)      NOT NULL,
    agreement_type        VARCHAR(50)   NOT NULL,
    agreement_version     VARCHAR(20)   DEFAULT NULL,
    selected_products     JSON          DEFAULT NULL,
    total_monthly_cost    DECIMAL(12,2) DEFAULT 0,
    total_onetime_cost    DECIMAL(12,2) DEFAULT 0,
    total_integration_cost DECIMAL(12,2) DEFAULT 0,
    agreement_text        LONGTEXT      DEFAULT NULL,
    terms_html            LONGTEXT      DEFAULT NULL,
    signed                BOOLEAN       NOT NULL DEFAULT FALSE,
    signed_at             TIMESTAMP     DEFAULT NULL,
    signature_name        VARCHAR(255)  DEFAULT NULL,
    ip_address            VARCHAR(45)   DEFAULT NULL,
    user_agent            TEXT          DEFAULT NULL,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ma_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    INDEX idx_ma_merchant (merchant_id)
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
    CONSTRAINT fk_txn_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    INDEX idx_txn_merchant (merchant_id),
    INDEX idx_txn_status (status),
    INDEX idx_txn_created (created_at DESC),
    INDEX idx_txn_id (transaction_id),
    INDEX idx_txn_settlement_status (settlement_status)
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
