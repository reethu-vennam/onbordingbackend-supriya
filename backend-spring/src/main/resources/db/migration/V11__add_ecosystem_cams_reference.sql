-- V11: Add ecosystem_cams_reference to merchant_profiles
-- Stores cp_mdt_ref_no from the mandatecreate response — the status-check endpoint
-- (/api/v1/ob/mandate-status) requires THIS value as `ref`, not our own trxnno
-- (confirmed 2026-09-23: querying by trxnno always returns NOT_FOUND).
-- Flyway is disabled (spring.flyway.enabled=false) so this must be run by hand.

ALTER TABLE merchant_profiles
ADD COLUMN IF NOT EXISTS ecosystem_cams_reference VARCHAR(100) DEFAULT NULL;
