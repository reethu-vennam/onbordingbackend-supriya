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
public class ChargebackSummaryResponse {

    private long totalChargebacks;
    private long pendingChargebacks;
    private long recoveredChargebacks;
    private BigDecimal totalAmount;
    private BigDecimal pendingAmount;
    private BigDecimal recoveredAmount;
    private BigDecimal totalChargebackAmount;
    private BigDecimal pendingChargebackAmount;
    private BigDecimal chargebackRecoveryAvailable;
}
