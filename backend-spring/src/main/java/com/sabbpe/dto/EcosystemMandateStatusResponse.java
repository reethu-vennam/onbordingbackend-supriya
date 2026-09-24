package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** status is one of: initiated | pending | active | failed (normalized from the ecosystem's MANDATE_* strings). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcosystemMandateStatusResponse {
    private String status;
    private String subscriptionId;
    private String nextDueDate;
}
