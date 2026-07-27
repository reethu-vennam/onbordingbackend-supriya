package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementResponse {

    private String id;
    private String merchantId;
    private String agreementType;
    private String agreementVersion;
    private String selectedProducts;
    private BigDecimal totalMonthlyCost;
    private BigDecimal totalOnetimeCost;
    private BigDecimal totalIntegrationCost;
    private boolean signed;
    private LocalDateTime signedAt;
    private String signatureName;
    private LocalDateTime createdAt;
}
