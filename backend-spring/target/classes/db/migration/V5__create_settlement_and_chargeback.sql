-- V5: Settlement engine, rolling reserve, chargebacks

CREATE TABLE IF NOT EXISTS settlement_history (
    id                   CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    merchant_id          CHAR(36)      NOT NULL,
    distributor_id       CHAR(36)      NOT NULL,
    settlement_batch_ref VARCHAR(100)  NOT NULL,
    settlement_date      DATE          NOT NULL DEFAULT (CURRENT_DATE),
    settlement_cycle_days SMALLINT     NOT NULL,
    gross_amount         DECIMAL(12,2) NOT NULL,
    mdr_deduction        DECIMAL(12,2) NOT NULL DEFAULT 0,
    rolling_reserve_held DECIMAL(12,2) NOT NULL DEFAULT 0,
    net_settlement_amount DECIMAL(12,2) NOT NULL,
    transaction_count    INT           NOT NULL,
    transaction_refs     JSON          NOT NULL DEFAULT '[]',
    status               VARCHAR(20)   NOT NULL DEFAULT 'pending',
    processed_at         TIMESTAMP     DEFAULT NULL,
    failure_reason       TEXT          DEFAULT NULL,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sh_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    INDEX idx_sh_merchant (merchant_id),
    INDEX idx_sh_distributor (distributor_id),
    INDEX idx_sh_date (settlement_date DESC),
    INDEX idx_sh_batch_ref (settlement_batch_ref),
    INDEX idx_sh_status (status),
    UNIQUE KEY uk_sh_batch (merchant_id, settlement_batch_ref)
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
    CONSTRAINT fk_rrl_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    INDEX idx_rrl_merchant (merchant_id),
    INDEX idx_rrl_distributor (distributor_id),
    INDEX idx_rrl_status (status),
    INDEX idx_rrl_release (release_date),
    INDEX idx_rrl_merchant_status (merchant_id, status),
    UNIQUE KEY uk_rrl_batch (merchant_id, transaction_ref)
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
    CONSTRAINT fk_cb_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    INDEX idx_cb_merchant (merchant_id),
    INDEX idx_cb_status (status),
    INDEX idx_cb_date (chargeback_date DESC)
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
    CONSTRAINT fk_cbh_chargeback FOREIGN KEY (chargeback_id) REFERENCES chargebacks(id) ON DELETE CASCADE,
    CONSTRAINT fk_cbh_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    INDEX idx_cbh_chargeback (chargeback_id),
    INDEX idx_cbh_merchant (merchant_id),
    INDEX idx_cbh_event (event_type),
    INDEX idx_cbh_timestamp (event_timestamp DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS distributor_recovery_history (
    id                  CHAR(36)      PRIMARY KEY DEFAULT (UUID()),
    distributor_id      CHAR(36)      NOT NULL,
    chargeback_id       CHAR(36)      NOT NULL,
    merchant_id         CHAR(36)      NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    recovery_source     VARCHAR(50)   NOT NULL DEFAULT 'distributor_balance',
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_drh_distributor FOREIGN KEY (distributor_id) REFERENCES distributor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_drh_chargeback FOREIGN KEY (chargeback_id) REFERENCES chargebacks(id) ON DELETE CASCADE,
    CONSTRAINT fk_drh_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE,
    UNIQUE KEY uk_drh_chargeback (distributor_id, chargeback_id),
    INDEX idx_drh_distributor (distributor_id),
    INDEX idx_drh_chargeback (chargeback_id),
    INDEX idx_drh_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Stored procedures (migrated from Supabase functions)

DELIMITER //

CREATE OR REPLACE FUNCTION increment_merchant_balances(
    p_merchant_id CHAR(36),
    p_pending_increment DECIMAL(12,2),
    p_total_increment DECIMAL(12,2)
) RETURNS VOID
BEGIN
    UPDATE merchant_profiles
    SET
        pending_settlement_amount = pending_settlement_amount + COALESCE(p_pending_increment, 0),
        total_settled_amount = total_settled_amount + COALESCE(p_total_increment, 0),
        last_settled_at = CURRENT_TIMESTAMP
    WHERE id = p_merchant_id;
END //

CREATE OR REPLACE FUNCTION update_chargeback_balances(
    p_merchant_id CHAR(36),
    p_chargeback_amount DECIMAL(12,2),
    p_recovered_amount DECIMAL(12,2)
) RETURNS VOID
BEGIN
    UPDATE merchant_profiles
    SET
        total_chargeback_amount = total_chargeback_amount + COALESCE(p_chargeback_amount, 0),
        pending_chargeback_amount = pending_chargeback_amount + COALESCE(p_chargeback_amount, 0),
        chargeback_recovery_available = chargeback_recovery_available + COALESCE(p_recovered_amount, 0)
    WHERE id = p_merchant_id;
END //

DELIMITER ;
