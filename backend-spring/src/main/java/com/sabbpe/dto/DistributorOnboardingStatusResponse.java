package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributorOnboardingStatusResponse {

    private String id;
    private String companyName;
    private String contactPerson;
    private String email;
    private String mobileNumber;
    private String agreementStatus;
    private String kycStatus;
    private boolean panVerified;
    private boolean aadhaarVerified;
    private boolean bankVerified;
    private LocalDateTime kycSubmittedAt;
    private LocalDateTime agreementSentAt;
    private LocalDateTime onboardingCompletedAt;
    private boolean canSubmitKyc;
    private boolean canSignAgreement;
}
