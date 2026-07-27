package com.sabbpe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {

    private String id;

    @JsonProperty("txn_id")
    private String txnId;

    @JsonProperty("order_reference")
    private String orderReference;

    @JsonProperty("merchant_id")
    private String merchantId;

    @JsonProperty("merchant_name")
    private String merchantName;

    @JsonProperty("merchant_email")
    private String merchantEmail;

    private BigDecimal amount;

    @JsonProperty("amount_requested")
    private BigDecimal amountRequested;

    private String currency;

    private String status;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("payment_provider")
    private String paymentProvider;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("completed_at")
    private LocalDateTime completedAt;

    @JsonProperty("deduction_percentage")
    private BigDecimal deductionPercentage;

    @JsonProperty("net_amount_debit")
    private BigDecimal netAmountDebit;

    @JsonProperty("commission_rate")
    private BigDecimal commissionRate;

    @JsonProperty("bank_ref_num")
    private String bankRefNum;

    private String mode;

    @JsonProperty("card_type")
    private String cardType;

    private String city;
    private String state;

    private String txnid;

    @JsonProperty("product_info")
    private String productInfo;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_email")
    private String customerEmail;

    @JsonProperty("card_number")
    private String cardNumber;

    @JsonProperty("upi_va")
    private String upiVa;

    @JsonProperty("recovery_amount")
    private BigDecimal recoveryAmount;

    @JsonProperty("recovery_percentage")
    private BigDecimal recoveryPercentage;

    @JsonProperty("chargeback_status")
    private String chargebackStatus;
}
