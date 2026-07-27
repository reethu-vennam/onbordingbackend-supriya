package com.sabbpe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "distributor_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DistributorProfileEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    private String userId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "contact_person", nullable = false)
    private String contactPerson;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "territory")
    private String territory;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "bank_account_holder")
    private String bankAccountHolder;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 20)
    private String bankIfsc;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "aadhaar_last4", length = 4)
    private String aadhaarLast4;

    @Column(name = "pan_document_path", columnDefinition = "TEXT")
    private String panDocumentPath;

    @Column(name = "aadhaar_document_path", columnDefinition = "TEXT")
    private String aadhaarDocumentPath;

    @Column(name = "profile_photo_path", columnDefinition = "TEXT")
    private String profilePhotoPath;

    @Column(name = "pan_verified", nullable = false)
    private Boolean panVerified = false;

    @Column(name = "aadhaar_verified", nullable = false)
    private Boolean aadhaarVerified = false;

    @Column(name = "bank_verified", nullable = false)
    private Boolean bankVerified = false;

    @Column(name = "kyc_updated_at")
    private LocalDateTime kycUpdatedAt;

    @Column(name = "bank_updated_at")
    private LocalDateTime bankUpdatedAt;

    @Column(name = "default_commission_rate", precision = 5, scale = 2)
    private BigDecimal defaultCommissionRate;

    @Column(name = "payout_cycle", length = 20)
    private String payoutCycle = "daily";

    @Column(name = "security_deposit", nullable = false, precision = 12, scale = 2)
    private BigDecimal securityDeposit = BigDecimal.ZERO;

    @Column(name = "available_recovery_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal availableRecoveryBalance = BigDecimal.ZERO;

    @Column(name = "total_recovered_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRecoveredAmount = BigDecimal.ZERO;

    @Column(name = "agreement_status", nullable = false, length = 30)
    private String agreementStatus = "pending";

    @Column(name = "agreement_file_path", columnDefinition = "TEXT")
    private String agreementFilePath;

    @Column(name = "agreement_sent_at")
    private LocalDateTime agreementSentAt;

    @Column(name = "agreement_sent_by", length = 36)
    private String agreementSentBy;

    @Column(name = "signed_agreement_path", columnDefinition = "TEXT")
    private String signedAgreementPath;

    @Column(name = "agreement_uploaded_at")
    private LocalDateTime agreementUploadedAt;

    @Column(name = "agreement_approved_at")
    private LocalDateTime agreementApprovedAt;

    @Column(name = "agreement_approved_by", length = 36)
    private String agreementApprovedBy;

    @Column(name = "agreement_rejection_reason", columnDefinition = "TEXT")
    private String agreementRejectionReason;

    @Column(name = "credentials_sent_at")
    private LocalDateTime credentialsSentAt;

    @Column(name = "credentials_sent_by", length = 36)
    private String credentialsSentBy;

    @Column(name = "onboarding_completed_at")
    private LocalDateTime onboardingCompletedAt;

    @Column(name = "onboarding_token")
    private String onboardingToken;

    @Column(name = "onboarding_token_expires_at")
    private LocalDateTime onboardingTokenExpiresAt;

    @Column(name = "onboarding_token_used_at")
    private LocalDateTime onboardingTokenUsedAt;

    @Column(name = "kyc_status", nullable = false, length = 30)
    private String kycStatus = "pending";

    @Column(name = "kyc_submitted_at")
    private LocalDateTime kycSubmittedAt;

    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;

    @Column(name = "kyc_verified_by", length = 36)
    private String kycVerifiedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
