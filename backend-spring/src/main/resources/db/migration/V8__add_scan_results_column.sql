-- V8: Add scan_results JSON column to merchant_profiles
-- Stores Quick Scan OCR extraction results for auto-filling onboarding forms

ALTER TABLE merchant_profiles
ADD COLUMN IF NOT EXISTS scan_results JSON DEFAULT NULL;
