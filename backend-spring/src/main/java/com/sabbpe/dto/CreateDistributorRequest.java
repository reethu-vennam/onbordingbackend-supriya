package com.sabbpe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDistributorRequest {

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("contact_person")
    private String contactPerson;

    private String email;

    @JsonProperty("mobile_number")
    private String mobileNumber;

    @JsonProperty("pan_number")
    private String panNumber;

    @JsonProperty("aadhaar_number")
    private String aadhaarNumber;

    @JsonProperty("bank_account_holder")
    private String bankAccountHolder;

    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("bank_account_number")
    private String bankAccountNumber;

    @JsonProperty("bank_ifsc")
    private String bankIfsc;

    private String address;
    private String city;
    private String state;
    private String pincode;

    @JsonProperty("default_commission_rate")
    private BigDecimal defaultCommissionRate;

    @JsonProperty("payout_cycle")
    private String payoutCycle;

    // These are sent as camelCase from frontend
    private String profilePhotoBase64;
    private String profilePhotoFileName;
    private String signedAgreementBase64;
    private String signedAgreementFileName;
    private String panFileBase64;
    private String panDocumentFilename;
}
