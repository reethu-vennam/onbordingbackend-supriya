-- =====================================================
-- V8: Ecosystem sync log for approved merchants
-- =====================================================

CREATE TABLE IF NOT EXISTS ecosystem_sync_log (
    id             CHAR(36)     PRIMARY KEY DEFAULT (UUID()),
    merchant_id    CHAR(36)     NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'FAILED',
    attempt_count  INT          NOT NULL DEFAULT 1,
    last_attempt   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_message  TEXT         DEFAULT NULL,
    payload        JSON         DEFAULT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_esl_merchant (merchant_id),
    INDEX idx_esl_status (status),
    CONSTRAINT fk_esl_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
