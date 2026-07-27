package com.sabbpe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "merchant_id", nullable = false, length = 36)
    private String merchantId;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 255)
    private String transactionId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_mobile", length = 20)
    private String customerMobile;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "settlement_status", nullable = false, length = 20)
    private String settlementStatus = "unsettled";

    @Column(name = "settlement_batch_id", length = 36)
    private String settlementBatchId;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "deduction_percentage", precision = 5, scale = 2)
    private BigDecimal deductionPercentage;

    @Column(name = "amount_final", precision = 12, scale = 2)
    private BigDecimal amountFinal;

    @Column(name = "bank_ref_num", length = 100)
    private String bankRefNum;

    @Column(name = "card_type", length = 50)
    private String cardType;

    @Column(name = "card_number", length = 20)
    private String cardNumber;

    @Column(name = "upi_va")
    private String upiVa;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "payment_source", length = 50)
    private String paymentSource;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
