package com.sabbpe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantProfileEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    private String userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "aadhaar_number", length = 20)
    private String aadhaarNumber;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "onboarding_status", nullable = false, length = 30)
    private String onboardingStatus = "draft";

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "distributor_id", length = 36)
    private String distributorId;

    @Column(name = "invited_via", length = 50)
    private String invitedVia;

    @Column(name = "invitation_token", length = 255)
    private String invitationToken;

    @Column(name = "commission", precision = 5, scale = 2)
    private BigDecimal commission;

    @Column(name = "business_address_line1")
    private String businessAddressLine1;

    @Column(name = "business_address_line2")
    private String businessAddressLine2;

    @Column(name = "business_city", length = 100)
    private String businessCity;

    @Column(name = "business_state", length = 100)
    private String businessState;

    @Column(name = "business_postal_code", length = 20)
    private String businessPostalCode;

    @Column(name = "business_country", length = 100)
    private String businessCountry = "India";

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "bank_application_id", length = 100)
    private String bankApplicationId;

    @Column(name = "bank_merchant_code", length = 100)
    private String bankMerchantCode;

    @Column(name = "bank_response", columnDefinition = "JSON")
    private String bankResponse;

    @Column(name = "decision_at")
    private LocalDateTime decisionAt;

    @Column(name = "application_id", length = 100, unique = true)
    private String applicationId;

    @Column(name = "upi_vpa", length = 255)
    private String upiVpa;

    @Column(name = "upi_qr_string", columnDefinition = "TEXT")
    private String upiQrString;

    @Column(name = "bank_decision_notes", columnDefinition = "TEXT")
    private String bankDecisionNotes;

    @Column(name = "bank_approved_at")
    private LocalDateTime bankApprovedAt;

    @Column(name = "upi_mandate_status", length = 20)
    private String upiMandateStatus;

    @Column(name = "upi_mandate_ref_no", length = 100)
    private String upiMandateRefNo;

    // The CAMS-assigned reference (cp_mdt_ref_no from the mandatecreate response) — the
    // status-check endpoint requires THIS, not our own trxnno/upi_mandate_ref_no above
    // (confirmed 2026-09-23: querying by trxnno always returns NOT_FOUND).
    @Column(name = "ecosystem_cams_reference", length = 100)
    private String ecosystemCamsReference;

    // Onboarding Team's ecosystem integration (ecosystemuat.sabbpe.com) — the merchant's
    // permanent identity there, and the first product subscription created once the
    // mandate above goes active. See ONBOARDING_TEAM_GUIDE.md / SabbpeEcosystemService.
    @Column(name = "ecosystem_organization_id", length = 100)
    private String ecosystemOrganizationId;

    @Column(name = "ecosystem_organization_code", length = 50)
    private String ecosystemOrganizationCode;

    @Column(name = "ecosystem_onboarded_at")
    private LocalDateTime ecosystemOnboardedAt;

    @Column(name = "ecosystem_subscription_id", length = 100)
    private String ecosystemSubscriptionId;

    @Column(name = "ecosystem_subscription_next_due_date", length = 20)
    private String ecosystemSubscriptionNextDueDate;

    // Day-of-month the mandate is configured to recur on (its executabledays at creation) —
    // reused as anchor_day for the first subscription so it matches the mandate's actual
    // schedule, not whatever day the merchant happened to finish authorizing it.
    @Column(name = "ecosystem_mandate_anchor_day")
    private Integer ecosystemMandateAnchorDay;

    @Column(name = "selected_products", columnDefinition = "JSON")
    private String selectedProducts;

    @Column(name = "scan_results", columnDefinition = "JSON")
    private String scanResults;

    @Column(name = "monthly_rental_cost", precision = 10, scale = 2)
    private BigDecimal monthlyRentalCost = BigDecimal.ZERO;

    @Column(name = "one_time_cost", precision = 10, scale = 2)
    private BigDecimal oneTimeCost = BigDecimal.ZERO;

    @Column(name = "agreement_signed", nullable = false)
    private Boolean agreementSigned = false;

    @Column(name = "agreement_signed_at")
    private LocalDateTime agreementSignedAt;

    @Column(name = "agreement_ip_address", length = 45)
    private String agreementIpAddress;

    @Column(name = "agreement_signature", columnDefinition = "TEXT")
    private String agreementSignature;

    @Column(name = "total_monthly_cost", precision = 12, scale = 2)
    private BigDecimal totalMonthlyCost = BigDecimal.ZERO;

    @Column(name = "total_onetime_cost", precision = 12, scale = 2)
    private BigDecimal totalOnetimeCost = BigDecimal.ZERO;

    @Column(name = "total_integration_cost", precision = 12, scale = 2)
    private BigDecimal totalIntegrationCost = BigDecimal.ZERO;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "settlement_type", length = 20)
    private String settlementType = "next_day";

    @Column(name = "approval_status", length = 20)
    private String approvalStatus = "pending";

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "cancelled_cheque_url", columnDefinition = "TEXT")
    private String cancelledChequeUrl;

    @Column(name = "pan_card_url", columnDefinition = "TEXT")
    private String panCardUrl;

    @Column(name = "aadhaar_card_url", columnDefinition = "TEXT")
    private String aadhaarCardUrl;

    @Column(name = "business_proof_url", columnDefinition = "TEXT")
    private String businessProofUrl;

    @Column(name = "verification_submitted", nullable = false)
    private Boolean verificationSubmitted = false;

    @Column(name = "cpv_submitted", nullable = false)
    private Boolean cpvSubmitted = false;

    @Column(name = "cpv_status", length = 30)
    private String cpvStatus;

    @Column(name = "cpv_video_path", columnDefinition = "TEXT")
    private String cpvVideoPath;

    @Column(name = "cpv_submitted_at")
    private LocalDateTime cpvSubmittedAt;

    @Column(name = "cpv_verified_at")
    private LocalDateTime cpvVerifiedAt;

    @Column(name = "cpv_verified_by", length = 36)
    private String cpvVerifiedBy;

    @Column(name = "cpv_rejection_reason", columnDefinition = "TEXT")
    private String cpvRejectionReason;

    @Column(name = "has_pg_product", nullable = false)
    private Boolean hasPgProduct = false;

    @Column(name = "commercials_accepted", nullable = false)
    private Boolean commercialsAccepted = false;

    @Column(name = "commercials_accepted_at")
    private LocalDateTime commercialsAcceptedAt;

    @Column(name = "bank_commercials", columnDefinition = "JSON")
    private String bankCommercials;

    @Column(name = "bank_commercials_set_at")
    private LocalDateTime bankCommercialsSetAt;

    @Column(name = "bank_commercials_set_by", length = 36)
    private String bankCommercialsSetBy;

    @Column(name = "pg_commercials_accepted", nullable = false)
    private Boolean pgCommercialsAccepted = false;

    @Column(name = "pg_agreement_signed", nullable = false)
    private Boolean pgAgreementSigned = false;

    @Column(name = "pg_agreement_signed_at")
    private LocalDateTime pgAgreementSignedAt;

    @Column(name = "pg_agreement_signature", columnDefinition = "TEXT")
    private String pgAgreementSignature;

    @Column(name = "agreement_link", columnDefinition = "TEXT")
    private String agreementLink;

    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "registration_details", columnDefinition = "JSON")
    private String registrationDetails;

    @Column(name = "split_settlement_enabled", nullable = false)
    private Boolean splitSettlementEnabled = false;

    @Column(name = "split_percentage", precision = 5, scale = 2)
    private BigDecimal splitPercentage;

    @Column(name = "split_account_number", length = 50)
    private String splitAccountNumber;

    @Column(name = "split_ifsc_code", length = 20)
    private String splitIfscCode;

    @Column(name = "split_payment_config", columnDefinition = "JSON")
    private String splitPaymentConfig;

    @Column(name = "txn_details", columnDefinition = "JSON")
    private String txnDetails;

    @Column(name = "rolling_reserve_enabled", nullable = false)
    private Boolean rollingReserveEnabled = false;

    @Column(name = "rolling_reserve_percentage", precision = 5, scale = 2)
    private BigDecimal rollingReservePercentage;

    @Column(name = "rolling_reserve_fixed_inr", precision = 12, scale = 2)
    private BigDecimal rollingReserveFixedInr;

    @Column(name = "settlement_cycle_days", nullable = false)
    private Short settlementCycleDays = 1;

    @Column(name = "settlement_terms_locked", nullable = false)
    private Boolean settlementTermsLocked = false;

    @Column(name = "settlement_config_overridden_by_admin", nullable = false)
    private Boolean settlementConfigOverriddenByAdmin = false;

    @Column(name = "settlement_config_overridden_at")
    private LocalDateTime settlementConfigOverriddenAt;

    @Column(name = "settlement_config_overridden_by", length = 36)
    private String settlementConfigOverriddenBy;

    @Column(name = "settlement_config_override_reason", columnDefinition = "TEXT")
    private String settlementConfigOverrideReason;

    @Column(name = "undertaking_pdf_url", columnDefinition = "TEXT")
    private String undertakingPdfUrl;

    @Column(name = "pending_settlement_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal pendingSettlementAmount = BigDecimal.ZERO;

    @Column(name = "total_settled_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSettledAmount = BigDecimal.ZERO;

    @Column(name = "last_settled_at")
    private LocalDateTime lastSettledAt;

    @Column(name = "total_chargeback_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalChargebackAmount = BigDecimal.ZERO;

    @Column(name = "pending_chargeback_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal pendingChargebackAmount = BigDecimal.ZERO;

    @Column(name = "chargeback_recovery_available", nullable = false, precision = 12, scale = 2)
    private BigDecimal chargebackRecoveryAvailable = BigDecimal.ZERO;

    @Column(name = "onboarding_score", nullable = false)
    private Integer onboardingScore = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
