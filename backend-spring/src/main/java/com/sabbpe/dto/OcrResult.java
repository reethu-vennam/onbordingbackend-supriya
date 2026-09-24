package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {
    private String panNumber;
    private String aadhaarNumber;
    private String extractedName;
    private String dateOfBirth;
    private int confidence;
    private String rawText;

    // GST certificate fields
    private String gstNumber;
    private String businessName;
    private String entityType;
    private String stateCode;
    private String state;

    // Cancelled cheque fields
    private String ifscCode;
    private String accountNumber;
    private String bankName;
    private String branchName;
    private String accountHolderName;

    // Aadhaar back address fields
    private String address;
    private String addressLine1;
    private String city;
    private String pincode;
}
