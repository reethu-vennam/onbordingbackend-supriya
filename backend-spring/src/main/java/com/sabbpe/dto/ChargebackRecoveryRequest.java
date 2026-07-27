package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargebackRecoveryRequest {

    private String chargebackId;
    private String performedBy;
}
