package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitMerchantOnboardingRequest {
    private String merchantProfileId;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String businessName;
    private String panNumber;
    private String aadhaarNumber;
    private String gstNumber;
    private String entityType;
    private List<Map<String, Object>> bankDetails;
    private Map<String, Object> kycData;
    private Map<String, Object> documents;
    private List<Map<String, Object>> persons;
    private List<Map<String, Object>> entityDocuments;
    private Boolean operatingAddressDifferent;
    private Map<String, Object> registeredAddress;
    private Map<String, Object> operatingAddress;
    private String doingBusinessDocPath;
    private List<String> selectedProducts;
    private String settlementType;
}
