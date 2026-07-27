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
@Table(name = "chargebacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChargebackEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "merchant_id", nullable = false, length = 36)
    private String merchantId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "pending";

    @Column(name = "chargeback_date", nullable = false)
    private LocalDateTime chargebackDate;

    @Column(name = "recovered_at")
    private LocalDateTime recoveredAt;

    @Column(name = "recovery_source", length = 50)
    private String recoverySource;

    @Column(name = "recovery_steps", nullable = false, columnDefinition = "JSON")
    private String recoverySteps = "[]";

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (chargebackDate == null) chargebackDate = LocalDateTime.now();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
