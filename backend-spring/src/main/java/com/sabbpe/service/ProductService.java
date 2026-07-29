package com.sabbpe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.dto.ProductResponse;
import com.sabbpe.dto.SubProductResponse;
import com.sabbpe.model.ProductCatalogEntity;
import com.sabbpe.model.ProductSubCatalogEntity;
import com.sabbpe.repository.ProductCatalogRepository;
import com.sabbpe.repository.ProductSubCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductCatalogRepository productCatalogRepository;
    private final ProductSubCatalogRepository subCatalogRepository;
    private final ObjectMapper objectMapper;

    public List<ProductResponse> getCatalog() {
        return productCatalogRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());
    }

    public List<SubProductResponse> getSubCatalog(String parentProductCode) {
        return subCatalogRepository
                .findByParentProductCodeAndIsActiveTrueOrderByDisplayOrderAsc(parentProductCode)
                .stream()
                .map(this::toSubProductResponse)
                .collect(Collectors.toList());
    }

    public BigDecimal calculateMonthlyCost(String selectedProductsJson) {
        return sumByPricingType(selectedProductsJson, "monthly");
    }

    public BigDecimal calculateOnetimeCost(String selectedProductsJson) {
        return sumByPricingType(selectedProductsJson, "onetime");
    }

    public BigDecimal calculateIntegrationCost(String selectedProductsJson) {
        return sumByPricingType(selectedProductsJson, "integration");
    }

    private BigDecimal sumByPricingType(String selectedProductsJson, String pricingType) {
        if (selectedProductsJson == null || selectedProductsJson.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            JsonNode root = objectMapper.readTree(selectedProductsJson);
            if (!root.isArray()) return BigDecimal.ZERO;

            BigDecimal total = BigDecimal.ZERO;
            for (JsonNode item : root) {
                String type = item.has("pricing_type") ? item.get("pricing_type").asText() : "";
                if (pricingType.equals(type)) {
                    double price = item.has("price") ? item.get("price").asDouble() : 0;
                    total = total.add(BigDecimal.valueOf(price));
                }
            }
            return total;
        } catch (Exception e) {
            log.warn("Failed to parse selected_products JSON for {} cost: {}", pricingType, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private ProductResponse toProductResponse(ProductCatalogEntity p) {
        return ProductResponse.builder()
                .id(p.getId())
                .productCode(p.getProductCode())
                .productName(p.getProductName())
                .productDescription(p.getProductDescription())
                .features(p.getFeatures())
                .price(p.getPrice())
                .priceType(p.getPriceType())
                .priceMonthlyMin(p.getPriceMonthlyMin())
                .priceMonthlyMax(p.getPriceMonthlyMax())
                .priceOnetimeMin(p.getPriceOnetimeMin())
                .priceOnetimeMax(p.getPriceOnetimeMax())
                .priceIntegrationFee(p.getPriceIntegrationFee())
                .displayPrice(p.getDisplayPrice())
                .displayPriceType(p.getDisplayPriceType())
                .pricingNote(p.getPricingNote())
                .productImageUrl(p.getProductImageUrl())
                .category(p.getCategory())
                .isActive(p.getIsActive() != null && p.getIsActive())
                .displayOrder(p.getDisplayOrder())
                .build();
    }

    private SubProductResponse toSubProductResponse(ProductSubCatalogEntity s) {
        return SubProductResponse.builder()
                .id(s.getId())
                .parentProductCode(s.getParentProductCode())
                .productCode(s.getProductCode())
                .productName(s.getProductName())
                .productDescription(s.getProductDescription())
                .price(s.getPrice())
                .isActive(s.getIsActive() != null && s.getIsActive())
                .displayOrder(s.getDisplayOrder())
                .build();
    }
}
