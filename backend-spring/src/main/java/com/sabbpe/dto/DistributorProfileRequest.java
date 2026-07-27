package com.sabbpe.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DistributorProfileRequest {

    private String companyName;
    private String contactPerson;
    private String email;
    private String mobileNumber;
    private String territory;
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
    private String panDocumentPath;
    private String aadhaarDocumentPath;
    private String profilePhotoPath;

    private BigDecimal defaultCommissionRate;
    private String payoutCycle;

    public static final List<String> VALID_PAYOUT_CYCLES = List.of("daily", "weekly", "biweekly", "monthly");
}
