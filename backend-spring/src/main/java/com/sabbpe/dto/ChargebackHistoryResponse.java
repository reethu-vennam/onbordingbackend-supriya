package com.sabbpe.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChargebackHistoryResponse {

    private String id;
    private String chargebackId;
    private String action;
    private String eventType;
    private BigDecimal recoveredAmount;
    private String recoverySource;
    private LocalDateTime eventTimestamp;
    private String performedBy;
    private String comments;
}
