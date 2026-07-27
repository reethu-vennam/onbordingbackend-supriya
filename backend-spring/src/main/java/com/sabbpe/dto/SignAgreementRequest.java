package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignAgreementRequest {

    private String agreementType;
    private String agreementVersion;
    private String signature;
    private String signatureName;
    private String ipAddress;
    private String userAgent;
    private String selectedProducts;

    public String getSignatureName() {
        return signatureName != null ? signatureName : signature;
    }
}
