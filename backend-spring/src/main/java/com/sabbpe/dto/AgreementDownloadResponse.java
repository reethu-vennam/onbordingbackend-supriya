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
public class AgreementDownloadResponse {
    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("agreement_status")
    private String agreementStatus;

    @JsonProperty("agreement_rejection_reason")
    private String agreementRejectionReason;

    @JsonProperty("agreement_download_url")
    private String agreementDownloadUrl;
}
