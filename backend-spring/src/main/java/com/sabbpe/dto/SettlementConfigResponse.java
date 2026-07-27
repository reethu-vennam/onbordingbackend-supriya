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
public class SettlementConfigResponse {

    private boolean rollingReserveEnabled;
    private BigDecimal rollingReservePercentage;
    private BigDecimal rollingReserveFixedInr;
    private short settlementCycleDays;
    private boolean settlementTermsLocked;
    private boolean overriddenByAdmin;
    private LocalDateTime overriddenAt;
    private String overrideReason;
}
