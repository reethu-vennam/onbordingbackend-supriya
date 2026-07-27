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
@Table(name = "rolling_reserve_ledger", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"merchant_id", "transaction_ref"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RollingReserveLedgerEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "merchant_id", nullable = false, length = 36)
    private String merchantId;

    @Column(name = "distributor_id", nullable = false, length = 36)
    private String distributorId;

    @Column(name = "transaction_ref", nullable = false, length = 255)
    private String transactionRef;

    @Column(name = "gross_settlement_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSettlementAmount;

    @Column(name = "reserve_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal reserveAmount;

    @Column(name = "reserve_date", nullable = false)
    private LocalDate reserveDate;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "held";

    @Column(name = "debit_reason", columnDefinition = "TEXT")
    private String debitReason;

    @Column(name = "settlement_cycle_days", nullable = false)
    private Short settlementCycleDays;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "debited_at")
    private LocalDateTime debitedAt;

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
