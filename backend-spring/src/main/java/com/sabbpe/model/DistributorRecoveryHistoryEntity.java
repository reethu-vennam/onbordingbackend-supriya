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
@Table(name = "distributor_recovery_history", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"distributor_id", "chargeback_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DistributorRecoveryHistoryEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "distributor_id", nullable = false, length = 36)
    private String distributorId;

    @Column(name = "chargeback_id", nullable = false, length = 36)
    private String chargebackId;

    @Column(name = "merchant_id", nullable = false, length = 36)
    private String merchantId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "recovery_source", nullable = false, length = 50)
    private String recoverySource = "distributor_balance";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
    }
}
