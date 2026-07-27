package com.sabbpe.admin.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_kyc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MerchantKycEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private String merchantId;

    @Column(name = "video_kyc_completed", nullable = false)
    private Boolean videoKycCompleted = false;

    @Column(name = "location_captured", nullable = false)
    private Boolean locationCaptured = false;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "video_kyc_file_path", columnDefinition = "TEXT")
    private String videoKycFilePath;

    @Column(name = "selfie_file_path", columnDefinition = "TEXT")
    private String selfieFilePath;

    @Column(name = "kyc_status", nullable = false, length = 30)
    private String kycStatus = "pending";

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by", length = 36)
    private String verifiedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "full_address", columnDefinition = "TEXT")
    private String fullAddress;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String pincode;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
