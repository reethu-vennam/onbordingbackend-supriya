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
@Table(name = "chargeback_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChargebackHistoryEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "chargeback_id", nullable = false, length = 36)
    private String chargebackId;

    @Column(name = "merchant_id", nullable = false, length = 36)
    private String merchantId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "previous_data", columnDefinition = "JSON")
    private String previousData;

    @Column(name = "current_data", columnDefinition = "JSON")
    private String currentData;

    @Column(name = "recovered_amount", precision = 12, scale = 2)
    private BigDecimal recoveredAmount;

    @Column(name = "recovery_source", length = 50)
    private String recoverySource;

    @Column(name = "recovery_details", columnDefinition = "JSON")
    private String recoveryDetails;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (eventTimestamp == null) eventTimestamp = LocalDateTime.now();
        createdAt = LocalDateTime.now();
    }
}
