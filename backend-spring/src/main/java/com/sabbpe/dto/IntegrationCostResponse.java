package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationCostResponse {

    private String merchantId;

    private String userId;

    private BigDecimal totalIntegrationCost;

    private BigDecimal totalMonthlyCost;

    private BigDecimal totalOnetimeCost;
}
