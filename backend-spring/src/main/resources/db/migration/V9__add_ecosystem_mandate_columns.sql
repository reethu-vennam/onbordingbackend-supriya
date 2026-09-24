-- V9: Add ecosystem integration columns to merchant_profiles
-- Backs SabbpeEcosystemService / EcosystemMandateController — the Onboarding Team's
-- onboard -> token -> mandate -> status -> subscription flow (ONBOARDING_TEAM_GUIDE.md).
-- Flyway is disabled (spring.flyway.enabled=false) so this must be run by hand.

ALTER TABLE merchant_profiles
ADD COLUMN IF NOT EXISTS ecosystem_organization_id VARCHAR(100) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS ecosystem_organization_code VARCHAR(50) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS ecosystem_onboarded_at DATETIME DEFAULT NULL,
ADD COLUMN IF NOT EXISTS ecosystem_subscription_id VARCHAR(100) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS ecosystem_subscription_next_due_date VARCHAR(20) DEFAULT NULL;
