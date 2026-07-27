package com.sabbpe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankValidationResponse {

    @JsonProperty("isValid")
    private boolean isValid;
    private String accountName;
    private String accountStatus;
    private String requestId;
    private String trackingRefNo;
    private String responseId;
    private String statusCode;
    private String status;
    private String message;
    private String error;
    private Object rawResponse;
}
