package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementConfigRequest {

    private Boolean rollingReserveEnabled;
    private BigDecimal rollingReservePercentage;
    private BigDecimal rollingReserveFixedInr;
    private Short settlementCycleDays;
    private String overrideReason;
}
