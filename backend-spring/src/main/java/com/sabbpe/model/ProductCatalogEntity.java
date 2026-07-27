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
@Table(name = "product_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCatalogEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;

    @Column(name = "features", columnDefinition = "JSON")
    private String features;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "price_type", length = 30)
    private String priceType;

    @Column(name = "price_monthly_min", precision = 12, scale = 2)
    private BigDecimal priceMonthlyMin;

    @Column(name = "price_monthly_max", precision = 12, scale = 2)
    private BigDecimal priceMonthlyMax;

    @Column(name = "price_onetime_min", precision = 12, scale = 2)
    private BigDecimal priceOnetimeMin;

    @Column(name = "price_onetime_max", precision = 12, scale = 2)
    private BigDecimal priceOnetimeMax;

    @Column(name = "price_integration_fee", precision = 12, scale = 2)
    private BigDecimal priceIntegrationFee;

    @Column(name = "price_amc", precision = 12, scale = 2)
    private BigDecimal priceAmc;

    @Column(name = "price_mid", precision = 12, scale = 2)
    private BigDecimal priceMid;

    @Column(name = "price_sim_cost_min", precision = 12, scale = 2)
    private BigDecimal priceSimCostMin;

    @Column(name = "price_sim_cost_max", precision = 12, scale = 2)
    private BigDecimal priceSimCostMax;

    @Column(name = "display_price", precision = 12, scale = 2)
    private BigDecimal displayPrice;

    @Column(name = "display_price_type", length = 30)
    private String displayPriceType;

    @Column(name = "pricing_note", columnDefinition = "TEXT")
    private String pricingNote;

    @Column(name = "product_image_url", columnDefinition = "TEXT")
    private String productImageUrl;

    @Column(name = "category", nullable = false, length = 50)
    private String category = "software";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

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
