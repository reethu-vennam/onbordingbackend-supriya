-- V6: Support multiple bank accounts per merchant
-- Remove UNIQUE constraint on merchant_id so a merchant can have multiple bank details

ALTER TABLE merchant_bank_details DROP FOREIGN KEY fk_mbd_merchant;
ALTER TABLE merchant_bank_details DROP INDEX merchant_id;
ALTER TABLE merchant_bank_details ADD INDEX idx_mbd_merchant_id (merchant_id);
ALTER TABLE merchant_bank_details ADD CONSTRAINT fk_mbd_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_profiles(id) ON DELETE CASCADE;
