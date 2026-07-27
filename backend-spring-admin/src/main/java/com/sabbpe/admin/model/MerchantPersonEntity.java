package com.sabbpe.admin.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_persons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MerchantPersonEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "is_authorized_signatory")
    private Boolean isAuthorizedSignatory = false;

    @Column(name = "sequence_order")
    private Integer sequenceOrder = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
