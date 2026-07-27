package com.sabbpe.service;

import com.sabbpe.dto.ProductResponse;
import com.sabbpe.dto.SubProductResponse;
import com.sabbpe.model.ProductCatalogEntity;
import com.sabbpe.model.ProductSubCatalogEntity;
import com.sabbpe.repository.ProductCatalogRepository;
import com.sabbpe.repository.ProductSubCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductCatalogRepository productCatalogRepository;
    private final ProductSubCatalogRepository subCatalogRepository;

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
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateOnetimeCost(String selectedProductsJson) {
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateIntegrationCost(String selectedProductsJson) {
        return BigDecimal.ZERO;
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
