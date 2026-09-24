package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UPI AutoPay mandates are authorized by a direct push to the payer's UPI app — there is no
 * browser page to redirect to. {@code message} is CAMS's own instruction text (e.g. "Please
 * authorise the mandate request using UPI APP now").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcosystemMandateCreateResponse {
    private String trxnno;
    private String message;
    private String camsReference;
}
