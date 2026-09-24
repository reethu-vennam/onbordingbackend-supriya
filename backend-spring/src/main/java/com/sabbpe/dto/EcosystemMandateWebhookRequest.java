package com.sabbpe.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/** Body of the ecosystem's POST /api/mandates/status-callback webhook. */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EcosystemMandateWebhookRequest {
    private String merchantOrganizationId;
    private String mandateId;
    private String mandateReference;
    private String mandateType;
    private String mandateStatus;
    private String validFrom;
    private String validTo;
}
