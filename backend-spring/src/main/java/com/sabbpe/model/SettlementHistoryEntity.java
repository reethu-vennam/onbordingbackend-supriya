package com.sabbpe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement_history", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"merchant_id", "settlement_batch_ref"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementHistoryEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "merchant_id", nullable = false, length = 36)
    private String merchantId;

    @Column(name = "distributor_id", nullable = false, length = 36)
    private String distributorId;

    @Column(name = "settlement_batch_ref", nullable = false, length = 100)
    private String settlementBatchRef;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "settlement_cycle_days", nullable = false)
    private Short settlementCycleDays;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "mdr_deduction", nullable = false, precision = 12, scale = 2)
    private BigDecimal mdrDeduction = BigDecimal.ZERO;

    @Column(name = "rolling_reserve_held", nullable = false, precision = 12, scale = 2)
    private BigDecimal rollingReserveHeld = BigDecimal.ZERO;

    @Column(name = "net_settlement_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSettlementAmount;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    @Column(name = "transaction_refs", nullable = false, columnDefinition = "JSON")
    private String transactionRefs;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

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
