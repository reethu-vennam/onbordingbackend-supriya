package com.sabbpe.admin.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantProfileEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column
    private String email;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "aadhaar_number")
    private String aadhaarNumber;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "onboarding_status", nullable = false, length = 30)
    private String onboardingStatus = "draft";

    @Column(name = "application_id", length = 50)
    private String applicationId;

    @Column(name = "onboarding_score")
    private Double onboardingScore;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "cpv_status", length = 30)
    private String cpvStatus;

    @Column(name = "cpv_video_path", columnDefinition = "TEXT")
    private String cpvVideoPath;

    @Column(name = "cpv_submitted")
    private Boolean cpvSubmitted = false;

    @Column(name = "cpv_submitted_at")
    private LocalDateTime cpvSubmittedAt;

    @Column(name = "cpv_verified_at")
    private LocalDateTime cpvVerifiedAt;

    @Column(name = "cpv_verified_by")
    private String cpvVerifiedBy;

    @Column(name = "cpv_rejection_reason", columnDefinition = "TEXT")
    private String cpvRejectionReason;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "selected_products", columnDefinition = "JSON")
    private String selectedProducts;

    @Column(name = "total_monthly_cost")
    private Double totalMonthlyCost;

    @Column(name = "total_onetime_cost")
    private Double totalOnetimeCost;

    @Column(name = "total_integration_cost")
    private Double totalIntegrationCost;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "cancelled_cheque_url", columnDefinition = "TEXT")
    private String cancelledChequeUrl;

    @Column(name = "pan_card_url", columnDefinition = "TEXT")
    private String panCardUrl;

    @Column(name = "aadhaar_card_url", columnDefinition = "TEXT")
    private String aadhaarCardUrl;

    @Column(name = "business_proof_url", columnDefinition = "TEXT")
    private String businessProofUrl;

    @Column(name = "registration_details", columnDefinition = "JSON")
    private String registrationDetails;

    @Column(name = "split_payment_config", columnDefinition = "JSON")
    private String splitPaymentConfig;

    @Column(name = "bank_application_id", length = 100)
    private String bankApplicationId;

    @Column(name = "bank_merchant_code", length = 100)
    private String bankMerchantCode;

    @Column(name = "bank_response", columnDefinition = "JSON")
    private String bankResponse;

    @Column(name = "decision_at")
    private LocalDateTime decisionAt;

    @Column(name = "bank_decision_notes", columnDefinition = "TEXT")
    private String bankDecisionNotes;

    @Column(name = "bank_approved_at")
    private LocalDateTime bankApprovedAt;

    @Column(name = "agreement_signed", nullable = false)
    private Boolean agreementSigned = false;

    @Column(name = "agreement_signed_at")
    private LocalDateTime agreementSignedAt;

    @Column(name = "agreement_ip_address", length = 45)
    private String agreementIpAddress;

    @Column(name = "agreement_signature", columnDefinition = "TEXT")
    private String agreementSignature;

    @Column(name = "bank_commercials", columnDefinition = "JSON")
    private String bankCommercials;

    @Column(name = "bank_commercials_set_at")
    private LocalDateTime bankCommercialsSetAt;

    @Column(name = "bank_commercials_set_by", length = 36)
    private String bankCommercialsSetBy;

    @Column(name = "agreement_link", columnDefinition = "TEXT")
    private String agreementLink;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
