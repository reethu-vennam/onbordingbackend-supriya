package com.sabbpe.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SettlementHistoryResponse {

    private String id;
    private String merchantId;
    private String settlementBatchRef;
    private LocalDate settlementDate;
    private BigDecimal grossAmount;
    private BigDecimal mdrDeduction;
    private BigDecimal rollingReserveHeld;
    private BigDecimal netSettlementAmount;
    private int transactionCount;
    private String status;
    private LocalDateTime processedAt;
}
