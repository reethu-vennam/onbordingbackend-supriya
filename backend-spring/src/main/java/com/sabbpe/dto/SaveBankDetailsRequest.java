package com.sabbpe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveBankDetailsRequest {

    @NotBlank
    @JsonProperty("merchantProfileId")
    private String merchantProfileId;

    @NotBlank
    @JsonProperty("accountNumber")
    private String accountNumber;

    @NotBlank
    @JsonProperty("ifscCode")
    private String ifscCode;

    @NotBlank
    @JsonProperty("bankName")
    private String bankName;

    @NotBlank
    @JsonProperty("accountHolderName")
    private String accountHolderName;
}
