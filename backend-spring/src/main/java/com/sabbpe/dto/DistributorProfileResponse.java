package com.sabbpe.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DistributorProfileResponse {

    private String id;
    private String userId;
    private String companyName;
    private String contactPerson;
    private String email;
    private String mobileNumber;
    private String territory;
    private boolean isActive;
    private String address;
    private String city;
    private String state;
    private String pincode;

    private String bankAccountHolder;
    private String bankName;
    private String bankAccountNumber;
    private String bankIfsc;

    private String panNumber;
    private String aadhaarLast4;
    private boolean panVerified;
    private boolean aadhaarVerified;
    private boolean bankVerified;

    private String panDocumentPath;
    private String aadhaarDocumentPath;
    private String profilePhotoPath;
    private String signedAgreementPath;

    private BigDecimal defaultCommissionRate;
    private String payoutCycle;
    private BigDecimal securityDeposit;
    private BigDecimal availableRecoveryBalance;

    private String agreementStatus;
    private String kycStatus;
    private LocalDateTime createdAt;
}
