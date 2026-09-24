-- V10: Add ecosystem_mandate_anchor_day to merchant_profiles
-- Stores the UPI AutoPay mandate's configured recurring day (its executabledays at
-- creation), so the first product's subscription anchor_day matches the mandate's actual
-- schedule instead of whichever day the merchant happened to finish authorizing it on.
-- Flyway is disabled (spring.flyway.enabled=false) so this must be run by hand.

ALTER TABLE merchant_profiles
ADD COLUMN IF NOT EXISTS ecosystem_mandate_anchor_day SMALLINT DEFAULT NULL;
