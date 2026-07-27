package com.sabbpe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_persons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantPersonEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "merchant_id", nullable = false, length = 36)
    private String merchantId;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "address_proof_type", length = 50)
    private String addressProofType;

    @Column(name = "is_authorized_signatory", nullable = false)
    private Boolean isAuthorizedSignatory = false;

    @Column(name = "sequence_order")
    private Integer sequenceOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
    }
}
