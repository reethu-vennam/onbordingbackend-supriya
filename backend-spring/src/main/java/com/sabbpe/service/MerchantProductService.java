package com.sabbpe.service;

import com.sabbpe.dto.*;
import com.sabbpe.exception.BadRequestException;
import com.sabbpe.exception.ResourceNotFoundException;
import com.sabbpe.model.*;
import com.sabbpe.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantProductService {

    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantSubProductRepository subProductRepository;
    private final MerchantAgreementRepository agreementRepository;
    private final ProductService productService;

    @Transactional
    public MerchantProfileResponse updateProducts(String userId, UpdateProductsRequest request) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));

        if (request.getSelectedProducts() != null) {
            merchant.setSelectedProducts(request.getSelectedProducts());
            merchant.setTotalMonthlyCost(productService.calculateMonthlyCost(request.getSelectedProducts()));
            merchant.setTotalOnetimeCost(productService.calculateOnetimeCost(request.getSelectedProducts()));
            merchant.setTotalIntegrationCost(productService.calculateIntegrationCost(request.getSelectedProducts()));
        }

        merchant = merchantProfileRepository.save(merchant);

        if (request.getSubProducts() != null) {
            subProductRepository.deleteByMerchantProfileId(merchant.getId());
            for (UpdateProductsRequest.SubProductSelection sel : request.getSubProducts()) {
                if (sel.getSubProductCodes() != null) {
                    for (String subCode : sel.getSubProductCodes()) {
                        MerchantSubProductEntity sub = new MerchantSubProductEntity();
                        sub.setId(UUID.randomUUID().toString());
                        sub.setMerchantProfileId(merchant.getId());
                        sub.setParentProductCode(sel.getParentProductCode());
                        sub.setSubProductCode(subCode);
                        subProductRepository.save(sub);
                    }
                }
            }
        }

        return buildMerchantResponse(merchant);
    }

    public String getSelectedProducts(String userId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));
        return merchant.getSelectedProducts();
    }

    public List<SubProductResponse> getSelectedSubProducts(String userId, String parentProductCode) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));

        return subProductRepository
                .findByMerchantProfileIdAndParentProductCode(merchant.getId(), parentProductCode)
                .stream()
                .map(sp -> SubProductResponse.builder()
                        .productCode(sp.getSubProductCode())
                        .parentProductCode(sp.getParentProductCode())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public AgreementResponse signAgreement(String userId, SignAgreementRequest request, String ipAddress) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));

        MerchantAgreementEntity agreement = new MerchantAgreementEntity();
        agreement.setId(UUID.randomUUID().toString());
        agreement.setMerchantId(merchant.getId());
        agreement.setAgreementType(request.getAgreementType() != null ? request.getAgreementType() : "PG_AGREEMENT");
        agreement.setAgreementVersion(request.getAgreementVersion());
        agreement.setSelectedProducts(request.getSelectedProducts() != null
                ? request.getSelectedProducts() : merchant.getSelectedProducts());
        agreement.setTotalMonthlyCost(merchant.getTotalMonthlyCost());
        agreement.setTotalOnetimeCost(merchant.getTotalOnetimeCost());
        agreement.setTotalIntegrationCost(merchant.getTotalIntegrationCost());
        agreement.setSignatureName(request.getSignatureName());
        agreement.setIpAddress(ipAddress != null ? ipAddress : request.getIpAddress());
        agreement.setUserAgent(request.getUserAgent());
        agreement.setSigned(true);
        agreement.setSignedAt(LocalDateTime.now());
        agreement = agreementRepository.save(agreement);

        merchant.setAgreementSigned(true);
        merchant.setAgreementSignedAt(LocalDateTime.now());
        merchant.setAgreementIpAddress(agreement.getIpAddress());
        merchant.setAgreementSignature(request.getSignatureName());
        merchantProfileRepository.save(merchant);

        return toAgreementResponse(agreement);
    }

    public List<AgreementResponse> getAgreements(String userId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));
        return agreementRepository.findByMerchantIdOrderByCreatedAtDesc(merchant.getId())
                .stream()
                .map(this::toAgreementResponse)
                .collect(Collectors.toList());
    }

    private AgreementResponse toAgreementResponse(MerchantAgreementEntity a) {
        return AgreementResponse.builder()
                .id(a.getId())
                .merchantId(a.getMerchantId())
                .agreementType(a.getAgreementType())
                .agreementVersion(a.getAgreementVersion())
                .selectedProducts(a.getSelectedProducts())
                .totalMonthlyCost(a.getTotalMonthlyCost())
                .totalOnetimeCost(a.getTotalOnetimeCost())
                .totalIntegrationCost(a.getTotalIntegrationCost())
                .signed(a.getSigned() != null && a.getSigned())
                .signedAt(a.getSignedAt())
                .signatureName(a.getSignatureName())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private MerchantProfileResponse buildMerchantResponse(MerchantProfileEntity m) {
        return MerchantProfileResponse.builder()
                .id(m.getId())
                .userId(m.getUserId())
                .selectedProducts(m.getSelectedProducts())
                .totalMonthlyCost(m.getTotalMonthlyCost())
                .totalOnetimeCost(m.getTotalOnetimeCost())
                .totalIntegrationCost(m.getTotalIntegrationCost())
                .build();
    }
}
