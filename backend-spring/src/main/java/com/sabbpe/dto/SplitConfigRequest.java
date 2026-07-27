package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SplitConfigRequest {

    private Boolean splitSettlementEnabled;
    private BigDecimal splitPercentage;
    private String splitAccountNumber;
    private String splitIfscCode;
}
