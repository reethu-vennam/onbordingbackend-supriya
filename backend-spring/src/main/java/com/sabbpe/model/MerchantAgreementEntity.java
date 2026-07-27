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
@Table(name = "merchant_agreements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAgreementEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "merchant_id", nullable = false, length = 36)
    private String merchantId;

    @Column(name = "agreement_type", nullable = false, length = 50)
    private String agreementType;

    @Column(name = "agreement_version", length = 20)
    private String agreementVersion;

    @Column(name = "selected_products", columnDefinition = "JSON")
    private String selectedProducts;

    @Column(name = "total_monthly_cost", precision = 12, scale = 2)
    private BigDecimal totalMonthlyCost = BigDecimal.ZERO;

    @Column(name = "total_onetime_cost", precision = 12, scale = 2)
    private BigDecimal totalOnetimeCost = BigDecimal.ZERO;

    @Column(name = "total_integration_cost", precision = 12, scale = 2)
    private BigDecimal totalIntegrationCost = BigDecimal.ZERO;

    @Column(name = "agreement_text", columnDefinition = "LONGTEXT")
    private String agreementText;

    @Column(name = "terms_html", columnDefinition = "LONGTEXT")
    private String termsHtml;

    @Column(name = "signed", nullable = false)
    private Boolean signed = false;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signature_name")
    private String signatureName;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

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
