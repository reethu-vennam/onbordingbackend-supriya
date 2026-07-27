package com.sabbpe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private String id;
    private String productCode;
    private String productName;
    private String productDescription;
    private String features;
    private BigDecimal price;
    private String priceType;
    private BigDecimal priceMonthlyMin;
    private BigDecimal priceMonthlyMax;
    private BigDecimal priceOnetimeMin;
    private BigDecimal priceOnetimeMax;
    private BigDecimal priceIntegrationFee;
    private BigDecimal displayPrice;
    private String displayPriceType;
    private String pricingNote;
    private String productImageUrl;
    private String category;
    private boolean isActive;
    private Integer displayOrder;
}
