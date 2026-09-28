package com.sabbpe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final ObjectMapper objectMapper;

    @Transactional
    public MerchantProfileResponse updateProducts(String userId, UpdateProductsRequest request) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));

        if (request.getSelectedProducts() != null) {
            merchant.setSelectedProducts(request.getSelectedProducts());
            merchant.setTotalMonthlyCost(productService.calculateMonthlyCost(request.getSelectedProducts()));
            merchant.setTotalOnetimeCost(productService.calculateOnetimeCost(request.getSelectedProducts()));
            merchant.setTotalIntegrationCost(productService.calculateIntegrationCost(request.getSelectedProducts()));
        }

        merchant = merchantProfileRepository.save(merchant);

        if (request.getSubProducts() != null) {
            Map<String, String> subProductsByCode = new LinkedHashMap<>();
            for (UpdateProductsRequest.SubProductSelection selection : request.getSubProducts()) {
                if (selection.getSubProductCodes() == null) {
                    continue;
                }
                for (String subCode : selection.getSubProductCodes()) {
                    String existingParent = subProductsByCode.putIfAbsent(subCode, selection.getParentProductCode());
                    if (existingParent != null && !Objects.equals(existingParent, selection.getParentProductCode())) {
                        throw new BadRequestException(
                                "Sub-product " + subCode + " cannot be selected under multiple parent products");
                    }
                }
            }

            subProductRepository.deleteByMerchantProfileId(merchant.getId());
            subProductRepository.flush();
            for (Map.Entry<String, String> selection : subProductsByCode.entrySet()) {
                subProductRepository.upsertSubProduct(
                        UUID.randomUUID().toString(),
                        merchant.getId(),
                        selection.getValue(),
                        selection.getKey(),
                        LocalDateTime.now());
            }
        }

        return buildMerchantResponse(merchant);
    }

    public String getSelectedProducts(String userId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));
        return merchant.getSelectedProducts();
    }

    /**
     * Looked up by email/mobile rather than userId — used by the Dashboard's
     * internal
     * cross-service call, which only knows the merchant's contact details (there's
     * no
     * shared merchant ID between the two systems).
     */
    public List<String> getSelectedProductCodesByContact(String email, String mobileNumber) {
        Optional<MerchantProfileEntity> merchant = findByContact(email, mobileNumber);
        if (merchant.isEmpty()) {
            return List.of();
        }

        return extractProductCodes(merchant.get().getSelectedProducts());
    }

    /**
     * Merges newly-added product codes into the merchant's selected_products, keyed
     * by
     * email/mobile like the read side above. Called only after the merchant's
     * payment for
     * those products has actually succeeded (see the Dashboard's payment-result
     * flow) —
     * not when they're merely added to the cart, so this never records something
     * unpaid.
     */
    @Transactional
    public List<String> addProductCodesByContact(String email, String mobileNumber, List<String> newProductCodes) {
        Optional<MerchantProfileEntity> merchantOpt = findByContact(email, mobileNumber);
        if (merchantOpt.isEmpty()) {
            throw new ResourceNotFoundException("Merchant", "email/mobile", email + "/" + mobileNumber);
        }

        MerchantProfileEntity merchant = merchantOpt.get();
        List<String> existingCodes = extractProductCodes(merchant.getSelectedProducts());

        try {
            // Preserve existing entries as-is (they may carry pricing_type/price/etc. that
            // extractProductCodes doesn't read); only append genuinely new codes.
            com.fasterxml.jackson.databind.node.ArrayNode array;
            String existingJson = merchant.getSelectedProducts();
            if (existingJson != null && !existingJson.isBlank()) {
                JsonNode existingRoot = objectMapper.readTree(existingJson);
                array = existingRoot.isArray()
                        ? ((com.fasterxml.jackson.databind.node.ArrayNode) existingRoot).deepCopy()
                        : objectMapper.createArrayNode();
            } else {
                array = objectMapper.createArrayNode();
            }

            Set<String> mergedCodes = new LinkedHashSet<>(existingCodes);
            for (String code : newProductCodes) {
                if (mergedCodes.add(code)) {
                    array.addObject().put("product_code", code);
                }
            }

            merchant.setSelectedProducts(objectMapper.writeValueAsString(array));
            merchantProfileRepository.save(merchant);
            return new ArrayList<>(mergedCodes);
        } catch (Exception e) {
            log.error("Failed to save merged selected_products for merchant {}: {}", merchant.getId(), e.getMessage());
            throw new RuntimeException("Failed to save selected products", e);
        }
    }

    private Optional<MerchantProfileEntity> findByContact(String email, String mobileNumber) {
        Optional<MerchantProfileEntity> merchant = Optional.empty();

        if (email != null && !email.isBlank()) {
            merchant = merchantProfileRepository.findByEmail(email.trim().toLowerCase());
        }
        if (merchant.isEmpty() && mobileNumber != null && !mobileNumber.isBlank()) {
            merchant = merchantProfileRepository.findByMobileNumber(mobileNumber.trim());
        }
        return merchant;
    }

    private List<String> extractProductCodes(String selectedProductsJson) {
        if (selectedProductsJson == null || selectedProductsJson.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(selectedProductsJson);
            if (!root.isArray()) {
                return List.of();
            }

            Set<String> codes = new LinkedHashSet<>();
            for (JsonNode item : root) {
                JsonNode codeNode = item.has("product_code") ? item.get("product_code") : item.get("productCode");
                if (codeNode != null && !codeNode.isNull()) {
                    codes.add(codeNode.asText());
                }
            }
            return new ArrayList<>(codes);
        } catch (Exception e) {
            log.warn("Failed to parse selected_products JSON: {}", e.getMessage());
            return List.of();
        }
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
                ? request.getSelectedProducts()
                : merchant.getSelectedProducts());
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
