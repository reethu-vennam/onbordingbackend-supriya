package com.sabbpe.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankValidationRequest {

    @NotBlank(message = "Account holder name is required")
    private String custName;

    @NotBlank(message = "IFSC code is required")
    private String custIfsc;

    @NotBlank(message = "Account number is required")
    private String custAcctNo;

    private String requestId;
    private String trackingRefNo;
    private String txnType;
}
