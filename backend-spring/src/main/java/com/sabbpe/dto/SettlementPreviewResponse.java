package com.sabbpe.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SettlementPreviewResponse {

    private String merchantId;
    private String merchantName;
    private String email;
    private List<TransactionItem> transactions;
    private BigDecimal grossAmount;
    private BigDecimal mdrDeduction;
    private BigDecimal rollingReserve;
    private BigDecimal netAmount;
    private int transactionCount;
    private boolean reserveEnabled;
    private BigDecimal reservePercentage;
    private int settlementCycleDays;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionItem {
        private String transactionId;
        private BigDecimal amount;
        private String status;
        private String createdAt;
    }
}
