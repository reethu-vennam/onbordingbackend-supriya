package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChargebackRequest {

    private String merchantId;
    private BigDecimal amount;
    private String reason;
    private String currency;
    private String metadata;
}
