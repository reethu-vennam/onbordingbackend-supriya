-- V7: Replace individual bank detail columns with JSON array column
-- Stores all bank accounts for a merchant in one row using bank_details_json
-- Idempotent — safe to re-run if partially applied

-- Step 1: Add the new JSON column (skip if already exists)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_bank_details' AND COLUMN_NAME = 'bank_details_json');
SET @sql_add_col = IF(@col_exists = 0,
    'ALTER TABLE merchant_bank_details ADD COLUMN bank_details_json JSON DEFAULT NULL AFTER account_holder_name',
    'SELECT ''Column bank_details_json already exists'' AS msg');
PREPARE stmt FROM @sql_add_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 2: Migrate existing data — for each merchant, aggregate all rows into a JSON array
UPDATE merchant_bank_details AS main
SET bank_details_json = (
    SELECT JSON_ARRAYAGG(
        JSON_OBJECT(
            'accountNumber', sub.account_number,
            'ifscCode', sub.ifsc_code,
            'bankName', sub.bank_name,
            'accountHolderName', sub.account_holder_name,
            'upiVpa', sub.upi_vpa,
            'upiQrString', sub.upi_qr_string
        )
    )
    FROM merchant_bank_details AS sub
    WHERE sub.merchant_id = main.merchant_id
)
WHERE bank_details_json IS NULL;

-- Step 3: Delete duplicate rows — keep only one row per merchant (the one with the lowest id)
DELETE m1 FROM merchant_bank_details m1
INNER JOIN merchant_bank_details m2
ON m1.merchant_id = m2.merchant_id AND m1.id > m2.id;

-- Step 4: Drop old individual columns (skip each if already dropped)
SET @col_acct = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_bank_details' AND COLUMN_NAME = 'account_number');
SET @col_ifsc = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_bank_details' AND COLUMN_NAME = 'ifsc_code');
SET @col_bank_name = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_bank_details' AND COLUMN_NAME = 'bank_name');
SET @col_upi_vpa = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_bank_details' AND COLUMN_NAME = 'upi_vpa');
SET @col_upi_qr = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_bank_details' AND COLUMN_NAME = 'upi_qr_string');

SET @drop_cols = CONCAT(
    IF(@col_acct > 0, ', DROP COLUMN account_number', ''),
    IF(@col_ifsc > 0, ', DROP COLUMN ifsc_code', ''),
    IF(@col_bank_name > 0, ', DROP COLUMN bank_name', ''),
    IF(@col_upi_vpa > 0, ', DROP COLUMN upi_vpa', ''),
    IF(@col_upi_qr > 0, ', DROP COLUMN upi_qr_string', '')
);
SET @drop_cols = IF(LENGTH(@drop_cols) > 0,
    CONCAT('ALTER TABLE merchant_bank_details', SUBSTRING(@drop_cols, 2)),
    'SELECT ''All old columns already dropped'' AS msg');
PREPARE stmt FROM @drop_cols;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 5: Re-add UNIQUE constraint on merchant_id (one row per merchant)
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_bank_details' AND INDEX_NAME = 'idx_mbd_merchant_id');

SET @fk_check = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_bank_details' AND CONSTRAINT_NAME = 'fk_mbd_merchant' AND CONSTRAINT_TYPE = 'FOREIGN KEY');

SET @uq_check = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_bank_details' AND CONSTRAINT_NAME = 'uq_mbd_merchant_id' AND CONSTRAINT_TYPE = 'UNIQUE');

-- Drop old index if it still exists
SET @drop_idx = IF(@idx_exists > 0, 'ALTER TABLE merchant_bank_details DROP INDEX idx_mbd_merchant_id', 'SELECT ''No idx_mbd_merchant_id to drop'' AS msg');
PREPARE stmt FROM @drop_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop old FK if it still exists
SET @drop_fk = IF(@fk_check > 0, 'ALTER TABLE merchant_bank_details DROP FOREIGN KEY fk_mbd_merchant', 'SELECT ''No fk_mbd_merchant to drop'' AS msg');
PREPARE stmt FROM @drop_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add UNIQUE index and FK if not already present
SET @add_uq_fk = IF(@uq_check = 0,
    'ALTER TABLE merchant_bank_details ADD UNIQUE INDEX uq_mbd_merchant_id (merchant_id)',
    'SELECT ''UNIQUE index already exists'' AS msg');
PREPARE stmt FROM @add_uq_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Re-add FK only if missing
SET @fk_check2 = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'merchant_bank_details' AND CONSTRAINT_NAME = 'fk_mbd_merchant' AND CONSTRAINT_TYPE = 'FOREIGN KEY');
SET @add_fk = IF(@fk_check2 = 0,
    'ALTER TABLE merchant_bank_details ADD CONSTRAINT fk_mbd_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE',
    'SELECT ''FK already exists'' AS msg');
PREPARE stmt FROM @add_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
