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
@Table(name = "merchant_kyc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantKycEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "merchant_id", nullable = false, unique = true, length = 36)
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

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by", length = 36)
    private String verifiedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "full_address", columnDefinition = "TEXT")
    private String fullAddress;

    @Column(name = "area")
    private String area;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "country", length = 100)
    private String country;

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
