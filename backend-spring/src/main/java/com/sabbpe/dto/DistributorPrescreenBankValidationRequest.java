package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistributorPrescreenBankValidationRequest {
    private String accountHolderName;
    private String ifscCode;
    private String accountNumber;
}
