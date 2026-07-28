package com.sabbpe.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MerchantProfileResponse {

    private String id;
    private String userId;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String panNumber;
    private String aadhaarNumber;
    private String businessName;
    private String gstNumber;
    private String entityType;
    private String onboardingStatus;
    private String distributorId;
    private BigDecimal commission;

    private String businessAddressLine1;
    private String businessAddressLine2;
    private String businessCity;
    private String businessState;
    private String businessPostalCode;
    private String businessCountry;

    private String selectedProducts;
    private BigDecimal totalMonthlyCost;
    private BigDecimal totalOnetimeCost;
    private BigDecimal totalIntegrationCost;

    private Boolean agreementSigned;
    private LocalDateTime agreementSignedAt;
    private String agreementIpAddress;
    private String agreementSignature;

    private Boolean rollingReserveEnabled;
    private BigDecimal rollingReservePercentage;
    private BigDecimal rollingReserveFixedInr;
    private Short settlementCycleDays;
    private Boolean settlementTermsLocked;

    private String bankApplicationId;
    private String bankMerchantCode;
    private String bankResponse;

    private LocalDateTime submittedAt;
    private LocalDateTime decisionAt;
    private String applicationId;

    private String upiVpa;
    private String upiQrString;
    private String bankDecisionNotes;
    private LocalDateTime bankApprovedAt;
    private String upiMandateStatus;
    private String upiMandateRefNo;

    private BigDecimal monthlyRentalCost;
    private BigDecimal oneTimeCost;

    private String settlementType;
    private String approvalStatus;
    private String reviewNotes;

    private String cancelledChequeUrl;
    private String panCardUrl;
    private String aadhaarCardUrl;
    private String businessProofUrl;

    private Boolean verificationSubmitted;
    private String cpvStatus;
    private String cpvVideoPath;
    private LocalDateTime cpvSubmittedAt;
    private LocalDateTime cpvVerifiedAt;
    private String cpvVerifiedBy;
    private String cpvRejectionReason;

    private Boolean hasPgProduct;
    private Boolean commercialsAccepted;
    private LocalDateTime commercialsAcceptedAt;
    private String bankCommercials;
    private LocalDateTime bankCommercialsSetAt;
    private String bankCommercialsSetBy;

    private Boolean pgCommercialsAccepted;
    private Boolean pgAgreementSigned;
    private LocalDateTime pgAgreementSignedAt;
    private String pgAgreementSignature;
    private String agreementLink;

    private String lastModifiedBy;
    private LocalDateTime lastModifiedAt;
    private String registrationDetails;
    private String splitPaymentConfig;
    private Integer onboardingScore;

    private String rejectionReason;
    private String riskLevel;

    private BigDecimal pendingSettlementAmount;
    private BigDecimal totalSettledAmount;
    private LocalDateTime lastSettledAt;

    private String transactionId;
    private String txnDetails;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private BankDetailDto bankDetails;
    private List<KycDto> kyc;
    private List<DocumentDto> documents;
    private List<PersonDto> persons;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankDetailDto {
        private String id;
        private String accountNumber;
        private String ifscCode;
        private String bankName;
        private String accountHolderName;
        private String upiVpa;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycDto {
        private String id;
        private Boolean videoKycCompleted;
        private Boolean locationCaptured;
        private String kycStatus;
        private String fullAddress;
        private String city;
        private String state;
        private String pincode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentDto {
        private String id;
        private String documentType;
        private String fileName;
        private String filePath;
        private Long fileSize;
        private String mimeType;
        private String status;
        private String docCategory;
        private LocalDateTime uploadedAt;
        private LocalDateTime verifiedAt;
        private String rejectionReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonDto {
        private String id;
        private String role;
        private String fullName;
        private String panNumber;
        private Boolean isAuthorizedSignatory;
        private Integer sequenceOrder;
    }
}
