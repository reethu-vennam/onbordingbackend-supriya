package com.sabbpe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.dto.*;
import com.sabbpe.exception.BadRequestException;
import com.sabbpe.exception.ResourceNotFoundException;
import com.sabbpe.model.*;
import com.sabbpe.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributorService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantBankDetailRepository bankDetailRepository;
    private final MerchantDocumentRepository documentRepository;
    private final MerchantPersonRepository personRepository;
    private final MerchantKycRepository kycRepository;
    private final MerchantSubProductRepository subProductRepository;
    private final TransactionRepository transactionRepository;
    private final DistributorProfileRepository distributorProfileRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransbankService transbankService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private static final BigDecimal COMPANY_CUTOFF_RATE = BigDecimal.ONE;

    private static final List<String> ALLOWED_ENTITY_TYPES = List.of(
            "proprietorship", "individual", "partnership", "llp", "pvt_ltd",
            "public_ltd", "trust", "society", "huf", "government_psu", "education"
    );

    private static final Map<String, String> ENTITY_TYPE_MAPPING = new HashMap<>();
    static {
        ENTITY_TYPE_MAPPING.put("sole_proprietor", "proprietorship");
        ENTITY_TYPE_MAPPING.put("sole_proprietorship", "proprietorship");
        ENTITY_TYPE_MAPPING.put("proprietor", "proprietorship");
        ENTITY_TYPE_MAPPING.put("sole", "proprietorship");
        ENTITY_TYPE_MAPPING.put("non_registered", "individual");
        ENTITY_TYPE_MAPPING.put("unregistered", "individual");
        ENTITY_TYPE_MAPPING.put("partnership_firm", "partnership");
        ENTITY_TYPE_MAPPING.put("limited_liability_partnership", "llp");
        ENTITY_TYPE_MAPPING.put("llp_firm", "llp");
        ENTITY_TYPE_MAPPING.put("private_limited", "pvt_ltd");
        ENTITY_TYPE_MAPPING.put("pvt_limited", "pvt_ltd");
        ENTITY_TYPE_MAPPING.put("pvt_ltd_llp", "pvt_ltd");
        ENTITY_TYPE_MAPPING.put("private", "pvt_ltd");
        ENTITY_TYPE_MAPPING.put("public_limited", "public_ltd");
        ENTITY_TYPE_MAPPING.put("public", "public_ltd");
        ENTITY_TYPE_MAPPING.put("foundation", "trust");
        ENTITY_TYPE_MAPPING.put("ngo", "trust");
        ENTITY_TYPE_MAPPING.put("charitable_trust", "trust");
        ENTITY_TYPE_MAPPING.put("hindu_undivided_family", "huf");
        ENTITY_TYPE_MAPPING.put("government", "government_psu");
        ENTITY_TYPE_MAPPING.put("psu", "government_psu");
        ENTITY_TYPE_MAPPING.put("govt", "government_psu");
        ENTITY_TYPE_MAPPING.put("school", "education");
        ENTITY_TYPE_MAPPING.put("college", "education");
        ENTITY_TYPE_MAPPING.put("university", "education");
        ENTITY_TYPE_MAPPING.put("institute", "education");
    }

    public String normaliseEntityType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String munged = raw.trim().toLowerCase().replaceAll("[\\s\\-]+", "_");
        if (ALLOWED_ENTITY_TYPES.contains(munged)) return munged;
        return ENTITY_TYPE_MAPPING.get(munged);
    }

    public DistributorProfileEntity getDistributorByUserId(String userId) {
        return distributorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor", "userId", userId));
    }

    @Transactional
    public Map<String, Object> createMerchant(String distributorUserId, CreateMerchantRequest request) {
        // Validate required fields
        if (request.getEmail() == null || request.getPassword() == null ||
                request.getFullName() == null || request.getMobileNumber() == null) {
            throw new BadRequestException("email, password, fullName, and mobileNumber are required", "MISSING_FIELDS");
        }
        if (request.getPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters", "WEAK_PASSWORD");
        }

        // Validate commission
        if (request.getCommission() != null && request.getCommission().compareTo(BigDecimal.ONE) <= 0) {
            throw new BadRequestException("Commission must be greater than 1%", "COMMISSION_TOO_LOW");
        }

        // Validate settlement & reserve terms
        boolean reserveEnabled = request.getRollingReserveEnabled() != null && request.getRollingReserveEnabled();
        int settlementDays = request.getSettlementCycleDays() != null ? request.getSettlementCycleDays() : 1;

        if (!List.of(1, 2, 3).contains(settlementDays)) {
            throw new BadRequestException("settlement_cycle_days must be 1, 2, or 3", "INVALID_SETTLEMENT_CYCLE");
        }

        if (reserveEnabled) {
            boolean hasPercentage = request.getRollingReservePercentage() != null && request.getRollingReservePercentage().compareTo(BigDecimal.ZERO) > 0;
            boolean hasFixed = request.getRollingReserveFixedInr() != null && request.getRollingReserveFixedInr().compareTo(BigDecimal.ZERO) > 0;
            if (!hasPercentage && !hasFixed) {
                throw new BadRequestException("When rolling reserve is enabled, provide either rolling_reserve_percentage or rolling_reserve_fixed_inr", "RESERVE_AMOUNT_REQUIRED");
            }
            if (hasPercentage && hasFixed) {
                throw new BadRequestException("Provide only one of rolling_reserve_percentage or rolling_reserve_fixed_inr, not both", "RESERVE_BOTH_NOT_ALLOWED");
            }
            if (hasPercentage && (request.getRollingReservePercentage().compareTo(new BigDecimal("0.01")) < 0 || request.getRollingReservePercentage().compareTo(new BigDecimal("50")) > 0)) {
                throw new BadRequestException("rolling_reserve_percentage must be between 0.01 and 50", "RESERVE_PERCENTAGE_RANGE");
            }
            if (hasFixed && request.getRollingReserveFixedInr().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("rolling_reserve_fixed_inr must be a positive number", "RESERVE_FIXED_INR_INVALID");
            }
        }

        // Normalise entity type
        String normalizedEntityType = normaliseEntityType(request.getEntityType());
        if (request.getEntityType() != null && normalizedEntityType == null) {
            throw new BadRequestException("Invalid entityType. Allowed: " + ALLOWED_ENTITY_TYPES, "INVALID_ENTITY_TYPE");
        }

        // Verify caller is a distributor or employee
        boolean isDistributorOrEmployee = distributorProfileRepository.findByUserId(distributorUserId).isPresent()
                || employeeProfileRepository.findByUserId(distributorUserId).isPresent();
        if (!isDistributorOrEmployee) {
            throw new ResourceNotFoundException("Distributor or Employee", "userId", distributorUserId);
        }

        // Create or find merchant user
        String email = request.getEmail().toLowerCase().trim();
        boolean existingUser = userRepository.existsByEmail(email);

        UserEntity merchantUser;
        if (existingUser) {
            merchantUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BadRequestException("Email already registered"));
        } else {
            merchantUser = new UserEntity();
            merchantUser.setId(UUID.randomUUID().toString());
            merchantUser.setEmail(email);
            merchantUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            merchantUser.setFullName(request.getFullName());
            merchantUser.setMobileNumber(request.getMobileNumber());
            merchantUser.setIsActive(true);
            merchantUser = userRepository.save(merchantUser);

            UserRoleEntity role = new UserRoleEntity();
            role.setId(UUID.randomUUID().toString());
            role.setUser(merchantUser);
            role.setRoleId("merchant");
            userRoleRepository.save(role);
        }

        // Create or update merchant profile
        String merchantUserId = merchantUser.getId();
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(merchantUserId).orElse(null);

        if (merchant != null) {
            merchant.setRollingReserveEnabled(reserveEnabled);
            merchant.setRollingReservePercentage(reserveEnabled ? request.getRollingReservePercentage() : null);
            merchant.setRollingReserveFixedInr(reserveEnabled ? request.getRollingReserveFixedInr() : null);
            merchant.setSettlementCycleDays((short) settlementDays);
            merchantProfileRepository.save(merchant);
        } else {
            merchant = new MerchantProfileEntity();
            merchant.setId(UUID.randomUUID().toString());
            merchant.setUserId(merchantUserId);
            merchant.setFullName(request.getFullName());
            merchant.setMobileNumber(request.getMobileNumber());
            merchant.setEmail(email);
            merchant.setDistributorId(distributorUserId);
            merchant.setBusinessName(request.getBusinessName());
            merchant.setEntityType(normalizedEntityType);
            merchant.setPanNumber(request.getPanNumber());
            merchant.setGstNumber(request.getGstNumber());
            merchant.setCommission(request.getCommission());
            merchant.setOnboardingStatus("draft");
            merchant.setRollingReserveEnabled(reserveEnabled);
            merchant.setRollingReservePercentage(reserveEnabled ? request.getRollingReservePercentage() : null);
            merchant.setRollingReserveFixedInr(reserveEnabled ? request.getRollingReserveFixedInr() : null);
            merchant.setSettlementCycleDays((short) settlementDays);
            merchant.setSettlementTermsLocked(false);
            merchant = merchantProfileRepository.save(merchant);
        }

        // Send credentials email (non-fatal)
        try {
            String creatorName = null;
            var distProfile = distributorProfileRepository.findByUserId(distributorUserId);
            if (distProfile.isPresent()) {
                creatorName = distProfile.get().getCompanyName();
            } else {
                var empProfile = employeeProfileRepository.findByUserId(distributorUserId);
                if (empProfile.isPresent()) {
                    creatorName = empProfile.get().getFullName();
                }
            }
            boolean emailSent = notificationService.sendMerchantCredentialsEmail(
                    email, request.getFullName(), request.getPassword(),
                    request.getCommission() != null ? request.getCommission().doubleValue() : null,
                    creatorName
            );
            if (!emailSent) {
                log.warn("Credentials email delivery failed for merchant: {}", email);
            }
        } catch (Exception e) {
            log.error("Failed to send credentials email to {}: {}", email, e.getMessage(), e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("merchantUserId", merchantUserId);
        result.put("merchantProfileId", merchant.getId());
        result.put("email", email);
        result.put("existingUser", existingUser);
        return result;
    }

    @Transactional
    public void saveBankDetails(String distributorUserId, String merchantProfileId,
                                 String accountNumber, String ifscCode, String bankName, String accountHolderName) {
        getMerchantAndVerifyDistributor(merchantProfileId, distributorUserId);

        MerchantBankDetailEntity bank = new MerchantBankDetailEntity();
        bank.setId(UUID.randomUUID().toString());
        bank.setMerchantId(merchantProfileId);
        bank.setAccountNumber(accountNumber);
        bank.setIfscCode(ifscCode);
        bank.setBankName(bankName);
        bank.setAccountHolderName(accountHolderName);
        bankDetailRepository.save(bank);
    }

    @Transactional
    public void submitMerchantOnboarding(String distributorUserId, SubmitMerchantOnboardingRequest request) {
        String merchantId = request.getMerchantProfileId();
        if (merchantId == null) {
            throw new BadRequestException("merchantProfileId is required", "MISSING_ID");
        }

        MerchantProfileEntity merchant = getMerchantAndVerifyDistributor(merchantId, distributorUserId);
        String normalizedEntityType = normaliseEntityType(request.getEntityType());

        // Step 1: Update merchant profile
        if (request.getFullName() != null) merchant.setFullName(request.getFullName());
        if (request.getMobileNumber() != null) merchant.setMobileNumber(request.getMobileNumber());
        if (request.getEmail() != null) merchant.setEmail(request.getEmail());
        if (request.getBusinessName() != null) merchant.setBusinessName(request.getBusinessName());
        if (request.getPanNumber() != null) merchant.setPanNumber(request.getPanNumber());
        if (request.getAadhaarNumber() != null) merchant.setAadhaarNumber(request.getAadhaarNumber());
        if (request.getGstNumber() != null) merchant.setGstNumber(request.getGstNumber());
        if (normalizedEntityType != null) merchant.setEntityType(normalizedEntityType);
        if (request.getSettlementType() != null) merchant.setSettlementType(request.getSettlementType());

        // Save registration details
        if (request.getRegisteredAddress() != null || request.getOperatingAddress() != null) {
            Map<String, Object> regDetails = new HashMap<>();
            if (request.getRegisteredAddress() != null) regDetails.put("registeredAddress", request.getRegisteredAddress());
            if (request.getOperatingAddress() != null) regDetails.put("operatingAddress", request.getOperatingAddress());
            regDetails.put("operatingAddressDifferent", request.getOperatingAddressDifferent() != null && request.getOperatingAddressDifferent());
            try {
                merchant.setRegistrationDetails(objectMapper.writeValueAsString(regDetails));
            } catch (Exception e) {
                log.warn("Failed to serialize registrationDetails", e);
            }
        }

        merchant.setOnboardingStatus("submitted");
        merchant.setSubmittedAt(LocalDateTime.now());
        merchant.setVerificationSubmitted(true);
        merchantProfileRepository.save(merchant);

        // Step 2: Save bank details
        if (request.getBankDetails() != null) {
            try {
                String acct = (String) request.getBankDetails().getOrDefault("accountNumber", "");
                String ifsc = (String) request.getBankDetails().getOrDefault("ifscCode", "");
                String bankName = (String) request.getBankDetails().getOrDefault("bankName", "");
                String holder = (String) request.getBankDetails().getOrDefault("accountHolderName", "");
                if (!acct.isBlank() && !ifsc.isBlank()) {
                    MerchantBankDetailEntity bank = new MerchantBankDetailEntity();
                    bank.setId(UUID.randomUUID().toString());
                    bank.setMerchantId(merchantId);
                    bank.setAccountNumber(acct);
                    bank.setIfscCode(ifsc);
                    bank.setBankName(bankName);
                    bank.setAccountHolderName(holder);
                    bankDetailRepository.save(bank);
                }
            } catch (Exception e) {
                log.warn("Failed to save bank details during submit: {}", e.getMessage());
            }
        }

        // Step 3: Save legacy documents
        if (request.getDocuments() != null) {
            Map<String, String> legacyDocTypeMap = Map.of(
                    "panCard", "pan_card",
                    "aadhaarCard", "aadhaar_card",
                    "cancelledCheque", "cancelled_cheque",
                    "businessProof", "business_proof",
                    "bankStatement", "bank_statement"
            );
            List<MerchantDocumentEntity> docsToSave = new ArrayList<>();
            for (var entry : request.getDocuments().entrySet()) {
                String key = entry.getKey();
                String docType = legacyDocTypeMap.getOrDefault(key, key);
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> docData = (Map<String, Object>) entry.getValue();
                    String path = (String) docData.get("path");
                    if (path != null && !path.isBlank()) {
                        MerchantDocumentEntity doc = new MerchantDocumentEntity();
                        doc.setId(UUID.randomUUID().toString());
                        doc.setMerchantId(merchantId);
                        doc.setDocumentType(docType);
                        doc.setFilePath(path);
                        doc.setFileName((String) docData.getOrDefault("name", path));
                        Object sizeObj = docData.get("size");
                        if (sizeObj instanceof Number) doc.setFileSize(((Number) sizeObj).longValue());
                        doc.setDocCategory(List.of("cancelled_cheque", "bank_statement").contains(docType) ? "bank" : "entity_reg");
                        docsToSave.add(doc);
                    }
                }
            }
            if (!docsToSave.isEmpty()) {
                documentRepository.saveAll(docsToSave);
            }
        }

        // Step 3a: Save entity-level documents
        if (request.getEntityDocuments() != null && !request.getEntityDocuments().isEmpty()) {
            List<MerchantDocumentEntity> entityDocs = new ArrayList<>();
            for (Map<String, Object> ed : request.getEntityDocuments()) {
                String path = (String) ed.get("filePath");
                String name = (String) ed.get("fileName");
                if (path != null && !path.isBlank()) {
                    MerchantDocumentEntity doc = new MerchantDocumentEntity();
                    doc.setId(UUID.randomUUID().toString());
                    doc.setMerchantId(merchantId);
                    doc.setDocumentType((String) ed.getOrDefault("docType", "entity_doc"));
                    doc.setFilePath(path);
                    doc.setFileName(name != null ? name : path);
                    Object sizeObj = ed.get("fileSize");
                    if (sizeObj instanceof Number) doc.setFileSize(((Number) sizeObj).longValue());
                    doc.setMimeType((String) ed.get("mimeType"));
                    doc.setDocCategory((String) ed.getOrDefault("docCategory", "entity_reg"));
                    entityDocs.add(doc);
                }
            }
            if (!entityDocs.isEmpty()) {
                documentRepository.saveAll(entityDocs);
            }
        }

        // Step 3b: Save doing business address doc
        if (request.getDoingBusinessDocPath() != null && !request.getDoingBusinessDocPath().isBlank()) {
            MerchantDocumentEntity doc = new MerchantDocumentEntity();
            doc.setId(UUID.randomUUID().toString());
            doc.setMerchantId(merchantId);
            doc.setDocumentType("doing_business");
            doc.setFilePath(request.getDoingBusinessDocPath());
            doc.setFileName(request.getDoingBusinessDocPath());
            doc.setDocCategory("doing_business");
            documentRepository.save(doc);
        }

        // Step 3c: Save merchant persons
        if (request.getPersons() != null && !request.getPersons().isEmpty()) {
            List<MerchantPersonEntity> persons = new ArrayList<>();
            for (Map<String, Object> p : request.getPersons()) {
                MerchantPersonEntity person = new MerchantPersonEntity();
                person.setId(UUID.randomUUID().toString());
                person.setMerchantId(merchantId);
                person.setRole((String) p.getOrDefault("role", "proprietor"));
                person.setFullName((String) p.getOrDefault("fullName", ""));
                person.setPanNumber((String) p.get("panNumber"));
                person.setAddressProofType((String) p.get("addressProofType"));
                Object isAuth = p.get("isAuthorizedSignatory");
                person.setIsAuthorizedSignatory(isAuth instanceof Boolean ? (Boolean) isAuth : false);
                Object seq = p.get("sequenceOrder");
                person.setSequenceOrder(seq instanceof Number ? ((Number) seq).intValue() : 0);
                persons.add(person);
            }
            personRepository.saveAll(persons);
        }

        // Step 4: Save KYC data
        if (request.getKycData() != null) {
            try {
                Map<String, Object> kyc = request.getKycData();
                MerchantKycEntity kycEntity = kycRepository.findByMerchantId(merchantId).orElse(null);
                if (kycEntity == null) {
                    kycEntity = new MerchantKycEntity();
                    kycEntity.setId(UUID.randomUUID().toString());
                    kycEntity.setMerchantId(merchantId);
                }
                Object videoCompleted = kyc.get("isVideoCompleted");
                kycEntity.setVideoKycCompleted(videoCompleted instanceof Boolean && (Boolean) videoCompleted);
                Object locationVerified = kyc.get("locationVerified");
                kycEntity.setLocationCaptured(locationVerified instanceof Boolean && (Boolean) locationVerified);
                kycEntity.setSelfieFilePath((String) kyc.get("selfieUrl"));
                Object lat = kyc.get("latitude");
                if (lat instanceof Number) kycEntity.setLatitude(BigDecimal.valueOf(((Number) lat).doubleValue()));
                Object lng = kyc.get("longitude");
                if (lng instanceof Number) kycEntity.setLongitude(BigDecimal.valueOf(((Number) lng).doubleValue()));
                kycEntity.setFullAddress((String) kyc.get("fullAddress"));
                kycEntity.setArea((String) kyc.get("area"));
                kycEntity.setCity((String) kyc.get("city"));
                kycEntity.setState((String) kyc.get("state"));
                kycEntity.setPincode((String) kyc.get("pincode"));
                kycEntity.setCountry((String) kyc.get("country"));
                kycEntity.setKycStatus("pending");
                kycRepository.save(kycEntity);
            } catch (Exception e) {
                log.warn("Failed to save KYC data: {}", e.getMessage());
            }
        }

        // Step 5: Save selected products
        if (request.getSelectedProducts() != null && !request.getSelectedProducts().isEmpty()) {
            try {
                merchant.setSelectedProducts(objectMapper.writeValueAsString(request.getSelectedProducts()));
            } catch (Exception e) {
                log.warn("Failed to serialize selectedProducts", e);
            }
            merchantProfileRepository.save(merchant);
        }

        // Notify admin
        try {
            notificationService.notifyAdminNewSubmission(
                    merchant.getFullName(), merchant.getBusinessName(),
                    merchant.getEmail(), merchant.getMobileNumber()
            );
        } catch (Exception e) {
            log.warn("Failed to notify admin: {}", e.getMessage());
        }
    }

    @Transactional
    public void deleteMerchant(String distributorUserId, String merchantId) {
        MerchantProfileEntity merchant = getMerchantAndVerifyDistributor(merchantId, distributorUserId);

        // Cascade delete related records
        documentRepository.findByMerchantId(merchantId).forEach(d -> documentRepository.delete(d));
        personRepository.findByMerchantIdOrderBySequenceOrderAsc(merchantId).forEach(p -> personRepository.delete(p));
        bankDetailRepository.findByMerchantId(merchantId).forEach(b -> bankDetailRepository.delete(b));
        kycRepository.findByMerchantId(merchantId).ifPresent(k -> kycRepository.delete(k));
        subProductRepository.findByMerchantProfileId(merchantId).forEach(sp -> subProductRepository.delete(sp));

        merchantProfileRepository.deleteById(merchantId);
    }

    public SettlementConfigResponse getSettlementConfig(String merchantId) {
        MerchantProfileEntity m = merchantProfileRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));
        return SettlementConfigResponse.builder()
                .rollingReserveEnabled(m.getRollingReserveEnabled() != null && m.getRollingReserveEnabled())
                .rollingReservePercentage(m.getRollingReservePercentage())
                .rollingReserveFixedInr(m.getRollingReserveFixedInr())
                .settlementCycleDays(m.getSettlementCycleDays() != null ? m.getSettlementCycleDays() : (short) 1)
                .settlementTermsLocked(m.getSettlementTermsLocked() != null && m.getSettlementTermsLocked())
                .overriddenByAdmin(m.getSettlementConfigOverriddenByAdmin() != null && m.getSettlementConfigOverriddenByAdmin())
                .overriddenAt(m.getSettlementConfigOverriddenAt())
                .overrideReason(m.getSettlementConfigOverrideReason())
                .build();
    }

    @Transactional
    public SettlementConfigResponse updateSettlementConfig(String merchantId,
                                                            SettlementConfigRequest request,
                                                            String performedBy,
                                                            boolean isAdminOverride) {
        MerchantProfileEntity m = merchantProfileRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));

        if (m.getSettlementTermsLocked() != null && m.getSettlementTermsLocked() && !isAdminOverride) {
            throw new BadRequestException("Settlement terms are locked");
        }

        // Validate settlement days
        if (request.getSettlementCycleDays() != null && !List.of(1, 2, 3).contains(request.getSettlementCycleDays())) {
            throw new BadRequestException("settlement_cycle_days must be 1, 2, or 3", "INVALID_SETTLEMENT_CYCLE");
        }

        // Validate rolling reserve
        if (request.getRollingReserveEnabled() != null && request.getRollingReserveEnabled()) {
            boolean hasPercentage = request.getRollingReservePercentage() != null && request.getRollingReservePercentage().compareTo(BigDecimal.ZERO) > 0;
            boolean hasFixed = request.getRollingReserveFixedInr() != null && request.getRollingReserveFixedInr().compareTo(BigDecimal.ZERO) > 0;
            if (!hasPercentage && !hasFixed) {
                throw new BadRequestException("When rolling reserve is enabled, provide either percentage or fixed amount", "RESERVE_AMOUNT_REQUIRED");
            }
            if (hasPercentage && hasFixed) {
                throw new BadRequestException("Provide only one of percentage or fixed amount, not both", "RESERVE_BOTH_NOT_ALLOWED");
            }
        }

        if (request.getRollingReserveEnabled() != null) m.setRollingReserveEnabled(request.getRollingReserveEnabled());
        if (request.getRollingReservePercentage() != null) m.setRollingReservePercentage(request.getRollingReservePercentage());
        if (request.getRollingReserveFixedInr() != null) m.setRollingReserveFixedInr(request.getRollingReserveFixedInr());
        if (request.getSettlementCycleDays() != null) m.setSettlementCycleDays(request.getSettlementCycleDays().shortValue());

        if (isAdminOverride) {
            m.setSettlementConfigOverriddenByAdmin(true);
            m.setSettlementConfigOverriddenAt(LocalDateTime.now());
            m.setSettlementConfigOverriddenBy(performedBy);
            m.setSettlementConfigOverrideReason(request.getOverrideReason());
        }

        merchantProfileRepository.save(m);
        return getSettlementConfig(merchantId);
    }

    public List<MerchantProfileResponse> getDistributorMerchants(String distributorUserId, String status) {
        return merchantProfileRepository.findByDistributorId(distributorUserId)
                .stream()
                .filter(m -> status == null || status.isBlank() || status.equals(m.getOnboardingStatus()))
                .map(this::buildMerchantResponse)
                .toList();
    }

    public List<MerchantProfileResponse> getAllMerchants(String status) {
        return merchantProfileRepository.findAll()
                .stream()
                .filter(m -> status == null || status.isBlank() || status.equals(m.getOnboardingStatus()))
                .map(this::buildMerchantResponse)
                .toList();
    }

    public Map<String, Object> getDistributorTransactions(String distributorUserId, boolean includeAll,
                                                          String statusFilter, String search,
                                                          String merchantIdFilter, String typeFilter,
                                                          String dateFrom, String dateTo) {
        List<MerchantProfileEntity> merchants = includeAll
                ? merchantProfileRepository.findAll()
                : merchantProfileRepository.findByDistributorId(distributorUserId);

        List<String> merchantIds = merchants.stream().map(MerchantProfileEntity::getId).toList();
        if (merchantIds.isEmpty()) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("data", List.of());
            emptyResult.put("summary", buildEmptySummary());
            return emptyResult;
        }

        Map<String, MerchantProfileEntity> merchantMap = merchants.stream()
                .collect(java.util.stream.Collectors.toMap(MerchantProfileEntity::getId, m -> m));

        List<TransactionEntity> allTx = transactionRepository.findByMerchantIdIn(merchantIds);

        List<TransactionResponse> txList = allTx.stream()
                .filter(tx -> statusFilter == null || statusFilter.isBlank()
                        || statusFilter.equalsIgnoreCase(tx.getStatus()))
                .filter(tx -> merchantIdFilter == null || merchantIdFilter.isBlank()
                        || merchantIdFilter.equals(tx.getMerchantId()))
                .filter(tx -> typeFilter == null || typeFilter.isBlank()
                        || typeFilter.equalsIgnoreCase(tx.getPaymentMethod()))
                .filter(tx -> {
                    if (dateFrom == null || dateFrom.isBlank()) return true;
                    try {
                        LocalDateTime from = LocalDateTime.parse(dateFrom + "T00:00:00");
                        return tx.getCreatedAt() != null && !tx.getCreatedAt().isBefore(from);
                    } catch (Exception e) { return true; }
                })
                .filter(tx -> {
                    if (dateTo == null || dateTo.isBlank()) return true;
                    try {
                        LocalDateTime to = LocalDateTime.parse(dateTo + "T23:59:59");
                        return tx.getCreatedAt() != null && !tx.getCreatedAt().isAfter(to);
                    } catch (Exception e) { return true; }
                })
                .filter(tx -> {
                    if (search == null || search.isBlank()) return true;
                    String q = search.toLowerCase();
                    return (tx.getTransactionId() != null && tx.getTransactionId().toLowerCase().contains(q))
                            || (tx.getCustomerName() != null && tx.getCustomerName().toLowerCase().contains(q))
                            || (tx.getCustomerEmail() != null && tx.getCustomerEmail().toLowerCase().contains(q));
                })
                .map(tx -> buildTransactionResponse(tx, merchantMap.get(tx.getMerchantId())))
                .toList();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAmount", txList.stream()
                .map(TransactionResponse::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue());
        summary.put("totalCount", txList.size());
        summary.put("successCount", (int) txList.stream().filter(tx -> "success".equalsIgnoreCase(tx.getStatus()) || "completed".equalsIgnoreCase(tx.getStatus())).count());
        summary.put("pendingCount", (int) txList.stream().filter(tx -> "pending".equalsIgnoreCase(tx.getStatus())).count());
        summary.put("failedCount", (int) txList.stream().filter(tx -> "failed".equalsIgnoreCase(tx.getStatus())).count());
        summary.put("cancelledCount", (int) txList.stream().filter(tx -> "cancelled".equalsIgnoreCase(tx.getStatus())).count());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("data", txList);
        resultMap.put("summary", summary);
        return resultMap;
    }

    private Map<String, Object> buildEmptySummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAmount", 0.0);
        summary.put("totalCount", 0);
        summary.put("successCount", 0);
        summary.put("pendingCount", 0);
        summary.put("failedCount", 0);
        summary.put("cancelledCount", 0);
        return summary;
    }

    private TransactionResponse buildTransactionResponse(TransactionEntity tx, MerchantProfileEntity merchant) {
        String merchantName = merchant != null ? merchant.getFullName() : null;
        String merchantEmail = merchant != null ? merchant.getEmail() : null;
        BigDecimal commissionRate = merchant != null ? merchant.getCommission() : null;

        BigDecimal deductionPct = tx.getDeductionPercentage() != null ? tx.getDeductionPercentage() : BigDecimal.ZERO;
        BigDecimal txnAmount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
        BigDecimal netAmountDebit = tx.getAmountFinal() != null ? tx.getAmountFinal() : txnAmount;

        return TransactionResponse.builder()
                .id(tx.getId())
                .txnId(tx.getTransactionId())
                .orderReference(tx.getTransactionId())
                .merchantId(tx.getMerchantId())
                .merchantName(merchantName)
                .merchantEmail(merchantEmail)
                .amount(txnAmount)
                .amountRequested(txnAmount)
                .currency(tx.getCurrency())
                .status(tx.getStatus())
                .paymentMethod(tx.getPaymentMethod())
                .paymentProvider("SpringBoot")
                .createdAt(tx.getCreatedAt())
                .completedAt(tx.getUpdatedAt())
                .deductionPercentage(deductionPct)
                .netAmountDebit(netAmountDebit)
                .commissionRate(commissionRate)
                .bankRefNum(tx.getBankRefNum())
                .mode(tx.getPaymentSource() != null ? tx.getPaymentSource() : tx.getPaymentMethod())
                .cardType(tx.getCardType())
                .cardNumber(tx.getCardNumber())
                .upiVa(tx.getUpiVa())
                .city(tx.getCity())
                .state(tx.getState())
                .customerName(tx.getCustomerName())
                .customerEmail(tx.getCustomerEmail())
                .build();
    }

    public Map<String, Object> getEarnings(String distributorUserId, boolean includeAll) {
        List<MerchantProfileEntity> merchants = includeAll
                ? merchantProfileRepository.findAll()
                : merchantProfileRepository.findByDistributorId(distributorUserId);

        List<Map<String, Object>> rows = merchants.stream().map(merchant -> {
            BigDecimal totalAmount = transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchant.getId())
                    .stream()
                    .filter(tx -> "success".equalsIgnoreCase(tx.getStatus()) || "completed".equalsIgnoreCase(tx.getStatus()))
                    .map(TransactionEntity::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal commissionRate = merchant.getCommission() != null ? merchant.getCommission() : BigDecimal.ZERO;
            BigDecimal payableRate = commissionRate.subtract(COMPANY_CUTOFF_RATE).max(BigDecimal.ZERO);
            BigDecimal commissionEarned = totalAmount.multiply(payableRate).divide(new BigDecimal("100"));

            Map<String, Object> row = new HashMap<>();
            row.put("client_id", merchant.getId());
            row.put("full_name", merchant.getFullName());
            row.put("email", merchant.getEmail());
            row.put("commission_rate", commissionRate);
            row.put("total_successful_amount", totalAmount);
            row.put("commission_earned", commissionEarned);
            return row;
        }).toList();

        BigDecimal totalEarnings = rows.stream()
                .map(row -> (BigDecimal) row.get("commission_earned"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSuccessfulAmount = rows.stream()
                .map(row -> (BigDecimal) row.get("total_successful_amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("data", rows);
        result.put("summary", Map.of(
                "totalEarnings", totalEarnings,
                "totalSuccessfulAmount", totalSuccessfulAmount,
                "merchantCount", rows.size()
        ));
        return result;
    }

    public BankValidationResponse validateVpa(String vpa) {
        return transbankService.validateBankAccount("VPA Holder", "SBIN0000001", "000000000000", null, null, null);
    }

    @Transactional
    public Map<String, Object> createDistributor(String adminUserId, CreateDistributorRequest request) {
        // Validate required fields
        if (request.getCompanyName() == null || request.getContactPerson() == null ||
                request.getEmail() == null || request.getMobileNumber() == null) {
            throw new BadRequestException("companyName, contactPerson, email, and mobileNumber are required", "MISSING_FIELDS");
        }

        // Validate PAN
        if (request.getPanNumber() != null && !request.getPanNumber().matches("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")) {
            throw new BadRequestException("Invalid PAN number format", "INVALID_PAN");
        }

        // Validate Aadhaar
        if (request.getAadhaarNumber() != null && !request.getAadhaarNumber().matches("\\d{12}")) {
            throw new BadRequestException("Aadhaar number must be 12 digits", "INVALID_AADHAAR");
        }

        // Validate payout_cycle
        if (request.getPayoutCycle() != null &&
                !List.of("weekly", "biweekly", "monthly", "daily").contains(request.getPayoutCycle())) {
            throw new BadRequestException("Invalid payout_cycle. Must be weekly, biweekly, monthly, or daily", "INVALID_PAYOUT_CYCLE");
        }

        // Check existing distributor by email
        String email = request.getEmail().toLowerCase().trim();
        if (distributorProfileRepository.findByEmail(email).isPresent() || userRepository.existsByEmail(email)) {
            throw new BadRequestException("A distributor with this email already exists", "DUPLICATE_EMAIL");
        }

        // Generate temp password
        String tempPassword = UUID.randomUUID().toString().substring(0, 12);

        // Create auth user
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setFullName(request.getContactPerson());
        user.setMobileNumber(request.getMobileNumber());
        user.setIsActive(true);
        user = userRepository.save(user);

        // Create user role
        UserRoleEntity role = new UserRoleEntity();
        role.setId(UUID.randomUUID().toString());
        role.setUser(user);
        role.setRoleId("distributor");
        userRoleRepository.save(role);

        // Save base64 files to disk
        String profilePhotoPath = null;
        String signedAgreementPath = null;
        String panDocumentPath = null;

        try {
            if (request.getProfilePhotoBase64() != null && request.getProfilePhotoFileName() != null) {
                profilePhotoPath = saveBase64File(request.getProfilePhotoBase64(),
                        request.getProfilePhotoFileName(), "profiles", user.getId());
            }
            if (request.getSignedAgreementBase64() != null && request.getSignedAgreementFileName() != null) {
                signedAgreementPath = saveBase64File(request.getSignedAgreementBase64(),
                        request.getSignedAgreementFileName(), "agreements", user.getId());
            }
            if (request.getPanFileBase64() != null && request.getPanDocumentFilename() != null) {
                panDocumentPath = saveBase64File(request.getPanFileBase64(),
                        request.getPanDocumentFilename(), "pan", user.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to save distributor files: {}", e.getMessage());
        }

        // Create distributor profile
        DistributorProfileEntity profile = new DistributorProfileEntity();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(user.getId());
        profile.setCompanyName(request.getCompanyName());
        profile.setContactPerson(request.getContactPerson());
        profile.setEmail(email);
        profile.setMobileNumber(request.getMobileNumber());
        profile.setPanNumber(request.getPanNumber());
        profile.setAadhaarLast4(request.getAadhaarNumber() != null ?
                request.getAadhaarNumber().substring(Math.max(0, request.getAadhaarNumber().length() - 4)) : null);
        profile.setBankAccountHolder(request.getBankAccountHolder());
        profile.setBankName(request.getBankName());
        profile.setBankAccountNumber(request.getBankAccountNumber());
        profile.setBankIfsc(request.getBankIfsc());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setState(request.getState());
        profile.setPincode(request.getPincode());
        profile.setDefaultCommissionRate(request.getDefaultCommissionRate());
        profile.setPayoutCycle(request.getPayoutCycle() != null ? request.getPayoutCycle() : "monthly");
        profile.setProfilePhotoPath(profilePhotoPath);
        profile.setSignedAgreementPath(signedAgreementPath);
        profile.setPanDocumentPath(panDocumentPath);
        profile.setAgreementStatus(signedAgreementPath != null ? "approved" : "pending");
        profile.setIsActive(false);
        profile = distributorProfileRepository.save(profile);

        // Send credentials email (non-fatal)
        try {
            notificationService.sendDistributorCredentialsEmail(email, request.getContactPerson(), tempPassword);
        } catch (Exception e) {
            log.warn("Failed to send credentials email: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", profile.getId());
        result.put("userId", user.getId());
        result.put("email", email);
        result.put("tempPassword", tempPassword);
        result.put("companyName", profile.getCompanyName());
        return result;
    }

    private String saveBase64File(String base64, String fileName, String subDir, String userId) throws Exception {
        byte[] fileBytes = Base64.getDecoder().decode(base64);
        String ext = "";
        if (fileName.contains(".")) {
            ext = fileName.substring(fileName.lastIndexOf("."));
        }
        String filename = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path uploadPath = Paths.get(uploadDir, subDir, userId);
        Files.createDirectories(uploadPath);
        Files.write(uploadPath.resolve(filename), fileBytes);
        return "/uploads/" + subDir + "/" + userId + "/" + filename;
    }

    private MerchantProfileEntity getMerchantAndVerifyDistributor(String merchantId, String distributorUserId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));
        if (!distributorUserId.equals(merchant.getDistributorId())) {
            throw new BadRequestException("Merchant does not belong to this distributor");
        }
        return merchant;
    }

    private MerchantProfileResponse buildMerchantResponse(MerchantProfileEntity m) {
        return MerchantProfileResponse.builder()
                .id(m.getId())
                .userId(m.getUserId())
                .fullName(m.getFullName())
                .mobileNumber(m.getMobileNumber())
                .email(m.getEmail())
                .businessName(m.getBusinessName())
                .onboardingStatus(m.getOnboardingStatus())
                .distributorId(m.getDistributorId())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

}
