package com.sabbpe.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EcosystemMandateCreateRequest {

    @NotBlank
    private String vpa;

    @NotBlank
    private String payerName;

    @NotBlank
    private String amount = "2.00";

    /** yyyy-MM-dd */
    @NotBlank
    private String startDate;

    /** yyyy-MM-dd */
    @NotBlank
    private String endDate;
}
