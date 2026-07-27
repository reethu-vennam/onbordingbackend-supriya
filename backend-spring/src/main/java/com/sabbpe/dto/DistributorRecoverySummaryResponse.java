package com.sabbpe.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DistributorRecoverySummaryResponse {

    private String distributorId;
    private BigDecimal availableRecoveryBalance;
    private BigDecimal totalRecoveredAmount;
    private BigDecimal securityDeposit;
    private long totalRecoveries;
}
