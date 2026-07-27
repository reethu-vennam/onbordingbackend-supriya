package com.sabbpe.admin.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_credit_checks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MerchantCreditCheckEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(nullable = false, length = 50)
    private String provider = "experian";

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "credit_score")
    private Integer creditScore;

    @Column(columnDefinition = "JSON")
    private String reportData;

    @Column(nullable = false, length = 30)
    private String status = "pending";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "checked_at", updatable = false)
    private LocalDateTime checkedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
