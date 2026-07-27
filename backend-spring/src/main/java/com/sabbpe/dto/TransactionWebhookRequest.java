package com.sabbpe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionWebhookRequest {

    @JsonProperty("transaction_id")
    private String transactionId;

    private String merchantId;

    private BigDecimal amount;

    private String currency;

    private String status;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("customer_mobile")
    private String customerMobile;

    private String metadata;
}
