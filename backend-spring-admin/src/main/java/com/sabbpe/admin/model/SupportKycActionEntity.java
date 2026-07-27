package com.sabbpe.admin.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_kyc_actions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportKycActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "support_staff_id", nullable = false)
    private String supportStaffId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
