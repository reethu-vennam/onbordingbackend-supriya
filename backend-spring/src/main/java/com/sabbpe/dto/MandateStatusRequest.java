package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MandateStatusRequest {

    private String upiMandateStatus;
    private String upiMandateRefNo;
}
