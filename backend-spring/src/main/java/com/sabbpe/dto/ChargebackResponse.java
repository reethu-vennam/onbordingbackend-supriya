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
public class ChargebackResponse {

    private String id;
    private String merchantId;
    private String merchantName;
    private BigDecimal amount;
    private String currency;
    private String reason;
    private String status;
    private LocalDateTime chargebackDate;
    private LocalDateTime recoveredAt;
    private String recoverySource;
    private LocalDateTime createdAt;
}
