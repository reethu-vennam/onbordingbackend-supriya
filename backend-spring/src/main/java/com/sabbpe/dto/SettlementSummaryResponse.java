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
public class SettlementSummaryResponse {

    private BigDecimal totalGrossSettled;
    private BigDecimal totalMdrDeducted;
    private BigDecimal totalReserveHeld;
    private BigDecimal totalNetSettled;
    private long totalSettlements;
    private BigDecimal pendingSettlementAmount;
    private BigDecimal totalSettledAmount;
}
