package com.sabbpe.service;

import com.sabbpe.dto.*;
import com.sabbpe.exception.BadRequestException;
import com.sabbpe.exception.ResourceNotFoundException;
import com.sabbpe.model.*;
import com.sabbpe.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantBankDetailRepository bankDetailRepository;
    private final MerchantDocumentRepository documentRepository;
    private final MerchantKycRepository kycRepository;
    private final MerchantPersonRepository personRepository;
    private final OnboardingAuditLogRepository auditLogRepository;
    private final ValidationService validationService;
    private final NotificationService notificationService;
    private final ProductService productService;

    private static final Set<String> VALID_STATUSES = Set.of(
            "draft", "submitted", "validating", "pending_bank_approval",
            "approved", "rejected", "validation_failed", "bank_rejected", "verified",
            "cpv_pending", "cpv_verified", "agreement_pending", "agreement_signed"
    );

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry("draft", Set.of("submitted")),
            Map.entry("submitted", Set.of("validating", "rejected", "cpv_pending")),
            Map.entry("validating", Set.of("pending_bank_approval", "validation_failed", "rejected")),
            Map.entry("validation_failed", Set.of("validating", "rejected")),
            Map.entry("cpv_pending", Set.of("cpv_verified", "pending_bank_approval", "rejected")),
            Map.entry("cpv_verified", Set.of("pending_bank_approval", "rejected")),
            Map.entry("pending_bank_approval", Set.of("approved", "bank_rejected", "rejected", "agreement_pending")),
            Map.entry("agreement_pending", Set.of("agreement_signed", "pending_bank_approval", "rejected")),
            Map.entry("agreement_signed", Set.of("approved", "bank_rejected", "rejected")),
            Map.entry("bank_rejected", Set.of("pending_bank_approval", "rejected")),
            Map.entry("approved", Set.<String>of()),
            Map.entry("rejected", Set.of("draft")),
            Map.entry("verified", Set.of("submitted"))
    );

    public MerchantProfileEntity getMerchantById(String merchantId) {
        return merchantProfileRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));
    }

    public MerchantProfileEntity getMerchantByUserId(String userId) {
        return merchantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", userId));
    }

    public MerchantProfileResponse getMerchantResponse(String merchantId) {
        MerchantProfileEntity merchant = getMerchantById(merchantId);
        return buildResponse(merchant);
    }

    public MerchantProfileResponse getMerchantResponseByUserId(String userId) {
        MerchantProfileEntity merchant = getMerchantByUserId(userId);
        return buildResponse(merchant);
    }

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public MerchantProfileResponse saveOrUpdateProfile(String userId, MerchantProfileRequest request) {
        MerchantProfileEntity merchant = merchantProfileRepository.findByUserId(userId).orElse(null);

        if (merchant == null) {
            merchant = new MerchantProfileEntity();
            merchant.setId(UUID.randomUUID().toString());
            merchant.setUserId(userId);
            merchant.setOnboardingStatus("draft");
        } else {
            if (!"draft".equals(merchant.getOnboardingStatus())
                    && !"rejected".equals(merchant.getOnboardingStatus())
                    && !"submitted".equals(merchant.getOnboardingStatus())) {
                throw new BadRequestException("Profile can only be edited in draft, rejected, or submitted status");
            }
        }

        updateMerchantFromRequest(merchant, request);
        merchant = merchantProfileRepository.save(merchant);

        if (request.getPersons() != null) {
            savePersons(merchant.getId(), request.getPersons());
        }
        if (request.getBankDetails() != null && !request.getBankDetails().isEmpty()) {
            saveBankDetails(merchant.getId(), request.getBankDetails());
        }
        if (request.getKyc() != null) {
            saveKyc(merchant.getId(), request.getKyc());
        }
        if (request.getDocuments() != null) {
            saveDocuments(merchant.getId(), request.getDocuments());
        }

        return buildResponse(merchant);
    }

    @Transactional
    public MerchantProfileResponse submitProfile(String userId) {
        MerchantProfileEntity merchant = getMerchantByUserId(userId);

        if (!"draft".equals(merchant.getOnboardingStatus())) {
            throw new BadRequestException("Only draft profiles can be submitted");
        }

        validationService.validateForSubmission(merchant);

        merchant.setOnboardingStatus("submitted");
        merchant.setSubmittedAt(LocalDateTime.now());
        merchant = merchantProfileRepository.save(merchant);

        auditLog(merchant.getId(), "SUBMIT", "draft", "submitted", userId, null);

        return buildResponse(merchant);
    }

    @Transactional
    public MerchantProfileResponse updateStatus(String merchantId, String newStatus,
                                                 String reason, String performedBy) {
        MerchantProfileEntity merchant = getMerchantById(merchantId);
        String oldStatus = merchant.getOnboardingStatus();

        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(oldStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new BadRequestException(
                    String.format("Invalid status transition from '%s' to '%s'", oldStatus, newStatus));
        }

        merchant.setOnboardingStatus(newStatus);
        if ("rejected".equals(newStatus) || "validation_failed".equals(newStatus)) {
            merchant.setRejectionReason(reason);
        }
        if ("approved".equals(newStatus)) {
            merchant.setReviewedAt(LocalDateTime.now());
            merchant.setReviewedBy(performedBy);
        }
        merchant = merchantProfileRepository.save(merchant);

        auditLog(merchantId, "STATUS_CHANGE", oldStatus, newStatus, performedBy, reason);

        try {
            if (merchant.getEmail() != null && !merchant.getEmail().isBlank()) {
                String merchantName = merchant.getFullName() != null ? merchant.getFullName() : "Merchant";
                if ("approved".equals(newStatus)) {
                    notificationService.sendEmail(merchant.getEmail(),
                            "Congratulations! Your SabbPe Account Has Been Approved",
                            String.format("""
                                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                        <div style="background: #059669; padding: 24px; text-align: center;">
                                            <h1 style="color: white; margin: 0;">Account Approved!</h1>
                                        </div>
                                        <div style="padding: 24px; background: #f9fafb;">
                                            <p>Dear %s,</p>
                                            <p>Congratulations! Your merchant account has been <b>approved</b>.</p>
                                            <p>You can now start accepting payments through SabbPe.</p>
                                            <p>Your account is now active. Log in to your dashboard to get started.</p>
                                            <a href="%s/merchant-onboarding?step=dashboard" style="display: inline-block; background: #059669; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 16px;">Go to Dashboard</a>
                                        </div>
                                    </div>
                                    """, merchantName, "http://localhost:8877"));
                } else if ("rejected".equals(newStatus) || "bank_rejected".equals(newStatus)) {
                    String reasonText = reason != null ? reason : "Please contact support for details.";
                    notificationService.sendEmail(merchant.getEmail(),
                            "SabbPe Onboarding - Application Update",
                            String.format("""
                                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                        <div style="background: #dc2626; padding: 24px; text-align: center;">
                                            <h1 style="color: white; margin: 0;">Application Update</h1>
                                        </div>
                                        <div style="padding: 24px; background: #f9fafb;">
                                            <p>Dear %s,</p>
                                            <p>We regret to inform you that your onboarding application has been <b>%s</b>.</p>
                                            <p><b>Reason:</b> %s</p>
                                            <p>Please contact our support team for assistance or try again.</p>
                                            <a href="%s/merchant-onboarding?step=dashboard" style="display: inline-block; background: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 16px;">View Dashboard</a>
                                        </div>
                                    </div>
                                    """, merchantName, newStatus, reasonText, "http://localhost:8877"));
                } else if ("cpv_pending".equals(newStatus)) {
                    notificationService.sendEmail(merchant.getEmail(),
                            "SabbPe - Shop Verification Required",
                            String.format("""
                                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                        <div style="background: #7c3aed; padding: 24px; text-align: center;">
                                            <h1 style="color: white; margin: 0;">Shop Verification Required</h1>
                                        </div>
                                        <div style="padding: 24px; background: #f9fafb;">
                                            <p>Dear %s,</p>
                                            <p>Your KYC has been verified. Please record a short video of your business premises (CPV) to proceed.</p>
                                            <a href="%s/merchant-onboarding?step=dashboard" style="display: inline-block; background: #7c3aed; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 16px;">Record Shop Video</a>
                                        </div>
                                    </div>
                                    """, merchantName, "http://localhost:8877"));
                } else if ("agreement_pending".equals(newStatus)) {
                    notificationService.sendEmail(merchant.getEmail(),
                            "SabbPe - Agreement Ready for Signing",
                            String.format("""
                                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                        <div style="background: #4f46e5; padding: 24px; text-align: center;">
                                            <h1 style="color: white; margin: 0;">Agreement Ready</h1>
                                        </div>
                                        <div style="padding: 24px; background: #f9fafb;">
                                            <p>Dear %s,</p>
                                            <p>The bank has sent your merchant agreement. Please review and sign it to proceed.</p>
                                            <a href="%s/merchant-onboarding?step=dashboard" style="display: inline-block; background: #4f46e5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 16px;">Sign Agreement</a>
                                        </div>
                                    </div>
                                    """, merchantName, "http://localhost:8877"));
                } else {
                    notificationService.notifyMerchantStatusChange(
                            merchant.getEmail(), merchantName, oldStatus, newStatus);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send status change email: {}", e.getMessage());
        }

        return buildResponse(merchant);
    }

    @Transactional
    public void deleteMerchant(String merchantId) {
        if (!merchantProfileRepository.existsById(merchantId)) {
            throw new ResourceNotFoundException("Merchant", "id", merchantId);
        }
        merchantProfileRepository.deleteById(merchantId);
    }

    public List<MerchantProfileResponse> getAllMerchants(String statusFilter) {
        List<MerchantProfileEntity> merchants;
        if (statusFilter != null && !statusFilter.isBlank()) {
            merchants = merchantProfileRepository.findByOnboardingStatus(statusFilter);
        } else {
            merchants = merchantProfileRepository.findAll();
        }
        return merchants.stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public List<MerchantProfileResponse> getMerchantsByDistributor(String distributorId, String statusFilter) {
        List<MerchantProfileEntity> merchants;
        if (statusFilter != null && !statusFilter.isBlank()) {
            merchants = merchantProfileRepository.findByDistributorIdAndOnboardingStatus(distributorId, statusFilter);
        } else {
            merchants = merchantProfileRepository.findByDistributorId(distributorId);
        }
        return merchants.stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public IntegrationCostResponse getIntegrationCost(String userId) {
        MerchantProfileEntity merchant = getMerchantByUserId(userId);

        BigDecimal integrationCost = productService.calculateIntegrationCost(merchant.getSelectedProducts());
        BigDecimal monthlyCost = productService.calculateMonthlyCost(merchant.getSelectedProducts());
        BigDecimal onetimeCost = productService.calculateOnetimeCost(merchant.getSelectedProducts());

        return IntegrationCostResponse.builder()
                .merchantId(merchant.getId())
                .userId(merchant.getUserId())
                .totalIntegrationCost(integrationCost)
                .totalMonthlyCost(monthlyCost)
                .totalOnetimeCost(onetimeCost)
                .build();
    }

    private void saveBankDetails(String merchantId, List<MerchantProfileRequest.BankDetailRequest> requests) {
        List<MerchantBankDetailEntity> existing = bankDetailRepository.findByMerchantId(merchantId);

        for (MerchantProfileRequest.BankDetailRequest req : requests) {
            validationService.validateBankDetailsRequest(
                    req.getAccountNumber(), req.getIfscCode(),
                    req.getAccountHolderName(), req.getBankName());

            MerchantBankDetailEntity bank = existing.stream()
                    .filter(e -> e.getAccountNumber().equals(req.getAccountNumber()) && e.getIfscCode().equals(req.getIfscCode()))
                    .findFirst().orElse(null);

            if (bank == null) {
                bank = new MerchantBankDetailEntity();
                bank.setId(UUID.randomUUID().toString());
                bank.setMerchantId(merchantId);
            }
            bank.setAccountNumber(req.getAccountNumber());
            bank.setIfscCode(req.getIfscCode());
            bank.setBankName(req.getBankName());
            bank.setAccountHolderName(req.getAccountHolderName());
            bank.setUpiVpa(req.getUpiVpa());
            try {
                bankDetailRepository.save(bank);
            } catch (DataIntegrityViolationException e) {
                log.warn("Could not save additional bank detail for merchant {} — DB may still have UNIQUE constraint. Run V6 migration.", merchantId);
            }
        }

        List<String> requestKeys = requests.stream()
                .map(r -> r.getAccountNumber() + "|" + r.getIfscCode())
                .collect(Collectors.toList());
        for (MerchantBankDetailEntity e : existing) {
            String key = e.getAccountNumber() + "|" + e.getIfscCode();
            if (!requestKeys.contains(key)) {
                bankDetailRepository.delete(e);
            }
        }
    }

    private void savePersons(String merchantId, List<MerchantProfileRequest.PersonRequest> persons) {
        personRepository.findByMerchantIdOrderBySequenceOrderAsc(merchantId)
                .forEach(p -> personRepository.delete(p));

        for (MerchantProfileRequest.PersonRequest req : persons) {
            MerchantPersonEntity person = new MerchantPersonEntity();
            person.setId(UUID.randomUUID().toString());
            person.setMerchantId(merchantId);
            person.setRole(req.getRole());
            person.setFullName(req.getFullName());
            person.setPanNumber(req.getPanNumber());
            person.setAddressProofType(req.getAddressProofType());
            person.setIsAuthorizedSignatory(req.getIsAuthorizedSignatory() != null ? req.getIsAuthorizedSignatory() : false);
            person.setSequenceOrder(req.getSequenceOrder() != null ? req.getSequenceOrder() : 0);
            personRepository.save(person);
        }
    }

    private void auditLog(String merchantId, String action, String oldStatus,
                          String newStatus, String performedBy, String notes) {
        OnboardingAuditLogEntity log = new OnboardingAuditLogEntity();
        log.setId(UUID.randomUUID().toString());
        log.setMerchantId(merchantId);
        log.setAction(action);
        log.setPreviousStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setPerformedBy(performedBy);
        log.setNotes(notes);
        auditLogRepository.save(log);
    }

    private void updateMerchantFromRequest(MerchantProfileEntity m, MerchantProfileRequest r) {
        if (r.getFullName() != null) m.setFullName(r.getFullName());
        if (r.getMobileNumber() != null) m.setMobileNumber(r.getMobileNumber());
        if (r.getEmail() != null) m.setEmail(r.getEmail());
        if (r.getPanNumber() != null) m.setPanNumber(r.getPanNumber().toUpperCase());
        if (r.getAadhaarNumber() != null) m.setAadhaarNumber(r.getAadhaarNumber());
        if (r.getBusinessName() != null) m.setBusinessName(r.getBusinessName());
        if (r.getGstNumber() != null) m.setGstNumber(r.getGstNumber().toUpperCase());
        if (r.getEntityType() != null) m.setEntityType(r.getEntityType());
        if (r.getBusinessAddressLine1() != null) m.setBusinessAddressLine1(r.getBusinessAddressLine1());
        if (r.getBusinessAddressLine2() != null) m.setBusinessAddressLine2(r.getBusinessAddressLine2());
        if (r.getBusinessCity() != null) m.setBusinessCity(r.getBusinessCity());
        if (r.getBusinessState() != null) m.setBusinessState(r.getBusinessState());
        if (r.getBusinessPostalCode() != null) m.setBusinessPostalCode(r.getBusinessPostalCode());
        if (r.getBusinessCountry() != null) m.setBusinessCountry(r.getBusinessCountry());
        if (r.getSelectedProducts() != null) m.setSelectedProducts(r.getSelectedProducts());
    }

    private MerchantProfileResponse buildResponse(MerchantProfileEntity m) {
        MerchantProfileResponse.MerchantProfileResponseBuilder builder = MerchantProfileResponse.builder()
                .id(m.getId())
                .userId(m.getUserId())
                .fullName(m.getFullName())
                .mobileNumber(m.getMobileNumber())
                .email(m.getEmail())
                .panNumber(m.getPanNumber())
                .aadhaarNumber(m.getAadhaarNumber())
                .businessName(m.getBusinessName())
                .gstNumber(m.getGstNumber())
                .entityType(m.getEntityType())
                .onboardingStatus(m.getOnboardingStatus())
                .distributorId(m.getDistributorId())
                .commission(m.getCommission())
                .businessAddressLine1(m.getBusinessAddressLine1())
                .businessAddressLine2(m.getBusinessAddressLine2())
                .businessCity(m.getBusinessCity())
                .businessState(m.getBusinessState())
                .businessPostalCode(m.getBusinessPostalCode())
                .businessCountry(m.getBusinessCountry())
                .selectedProducts(m.getSelectedProducts())
                .totalMonthlyCost(m.getTotalMonthlyCost())
                .totalOnetimeCost(m.getTotalOnetimeCost())
                .totalIntegrationCost(m.getTotalIntegrationCost())
                .agreementSigned(m.getAgreementSigned())
                .agreementSignedAt(m.getAgreementSignedAt())
                .agreementIpAddress(m.getAgreementIpAddress())
                .agreementSignature(m.getAgreementSignature())
                .rollingReserveEnabled(m.getRollingReserveEnabled())
                .rollingReservePercentage(m.getRollingReservePercentage())
                .rollingReserveFixedInr(m.getRollingReserveFixedInr())
                .settlementCycleDays(m.getSettlementCycleDays())
                .settlementTermsLocked(m.getSettlementTermsLocked())
                .bankApplicationId(m.getBankApplicationId())
                .bankMerchantCode(m.getBankMerchantCode())
                .bankResponse(m.getBankResponse())
                .submittedAt(m.getSubmittedAt())
                .decisionAt(m.getDecisionAt())
                .applicationId(m.getApplicationId())
                .upiVpa(m.getUpiVpa())
                .upiQrString(m.getUpiQrString())
                .bankDecisionNotes(m.getBankDecisionNotes())
                .bankApprovedAt(m.getBankApprovedAt())
                .upiMandateStatus(m.getUpiMandateStatus())
                .upiMandateRefNo(m.getUpiMandateRefNo())
                .monthlyRentalCost(m.getMonthlyRentalCost())
                .oneTimeCost(m.getOneTimeCost())
                .settlementType(m.getSettlementType())
                .approvalStatus(m.getApprovalStatus())
                .reviewNotes(m.getReviewNotes())
                .cancelledChequeUrl(m.getCancelledChequeUrl())
                .panCardUrl(m.getPanCardUrl())
                .aadhaarCardUrl(m.getAadhaarCardUrl())
                .businessProofUrl(m.getBusinessProofUrl())
                .verificationSubmitted(m.getVerificationSubmitted())
                .cpvStatus(m.getCpvStatus())
                .cpvVideoPath(m.getCpvVideoPath())
                .cpvSubmittedAt(m.getCpvSubmittedAt())
                .cpvVerifiedAt(m.getCpvVerifiedAt())
                .cpvVerifiedBy(m.getCpvVerifiedBy())
                .cpvRejectionReason(m.getCpvRejectionReason())
                .hasPgProduct(m.getHasPgProduct())
                .commercialsAccepted(m.getCommercialsAccepted())
                .commercialsAcceptedAt(m.getCommercialsAcceptedAt())
                .bankCommercials(m.getBankCommercials())
                .bankCommercialsSetAt(m.getBankCommercialsSetAt())
                .bankCommercialsSetBy(m.getBankCommercialsSetBy())
                .pgCommercialsAccepted(m.getPgCommercialsAccepted())
                .pgAgreementSigned(m.getPgAgreementSigned())
                .pgAgreementSignedAt(m.getPgAgreementSignedAt())
                .pgAgreementSignature(m.getPgAgreementSignature())
                .agreementLink(m.getAgreementLink())
                .lastModifiedBy(m.getLastModifiedBy())
                .lastModifiedAt(m.getLastModifiedAt())
                .registrationDetails(m.getRegistrationDetails())
                .splitPaymentConfig(m.getSplitPaymentConfig())
                .onboardingScore(m.getOnboardingScore())
                .rejectionReason(m.getRejectionReason())
                .riskLevel(m.getRiskLevel())
                .pendingSettlementAmount(m.getPendingSettlementAmount())
                .totalSettledAmount(m.getTotalSettledAmount())
                .lastSettledAt(m.getLastSettledAt())
                .transactionId(m.getTransactionId())
                .txnDetails(m.getTxnDetails())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt());

        try {
            List<MerchantBankDetailEntity> banks = bankDetailRepository.findByMerchantId(m.getId());
            if (!banks.isEmpty()) {
                List<MerchantProfileResponse.BankDetailDto> bankDtos = banks.stream()
                        .map(bank -> MerchantProfileResponse.BankDetailDto.builder()
                                .id(bank.getId())
                                .accountNumber(maskString(bank.getAccountNumber()))
                                .ifscCode(bank.getIfscCode())
                                .bankName(bank.getBankName())
                                .accountHolderName(bank.getAccountHolderName())
                                .upiVpa(bank.getUpiVpa())
                                .build())
                        .collect(Collectors.toList());
                builder.bankDetails(bankDtos);
            }
        } catch (Exception e) {
            log.debug("No bank details for merchant {}", m.getId());
        }

        try {
            List<MerchantProfileResponse.DocumentDto> docs = documentRepository.findByMerchantId(m.getId())
                    .stream().map(d -> MerchantProfileResponse.DocumentDto.builder()
                            .id(d.getId())
                            .documentType(d.getDocumentType())
                            .fileName(d.getFileName())
                            .filePath(d.getFilePath())
                            .fileSize(d.getFileSize())
                            .mimeType(d.getMimeType())
                            .status(d.getStatus())
                            .docCategory(d.getDocCategory())
                            .uploadedAt(d.getUploadedAt())
                            .verifiedAt(d.getVerifiedAt())
                            .rejectionReason(d.getRejectionReason())
                            .build())
                    .collect(Collectors.toList());
            builder.documents(docs);
        } catch (Exception e) {
            log.debug("No documents for merchant {}", m.getId());
        }

        try {
            MerchantKycEntity kyc = kycRepository.findByMerchantId(m.getId()).orElse(null);
            if (kyc != null) {
                builder.kyc(List.of(MerchantProfileResponse.KycDto.builder()
                        .id(kyc.getId())
                        .videoKycCompleted(kyc.getVideoKycCompleted())
                        .locationCaptured(kyc.getLocationCaptured())
                        .kycStatus(kyc.getKycStatus())
                        .fullAddress(kyc.getFullAddress())
                        .city(kyc.getCity())
                        .state(kyc.getState())
                        .pincode(kyc.getPincode())
                        .build()));
            }
        } catch (Exception e) {
            log.debug("No KYC for merchant {}", m.getId());
        }

        try {
            List<MerchantProfileResponse.PersonDto> personDtos = personRepository
                    .findByMerchantIdOrderBySequenceOrderAsc(m.getId())
                    .stream().map(p -> MerchantProfileResponse.PersonDto.builder()
                            .id(p.getId())
                            .role(p.getRole())
                            .fullName(p.getFullName())
                            .panNumber(p.getPanNumber())
                            .isAuthorizedSignatory(p.getIsAuthorizedSignatory())
                            .sequenceOrder(p.getSequenceOrder())
                            .build())
                    .collect(Collectors.toList());
            builder.persons(personDtos);
        } catch (Exception e) {
            log.debug("No persons for merchant {}", m.getId());
        }

        return builder.build();
    }

    @Transactional
    public void restartOnboarding(String userId) {
        MerchantProfileEntity merchant = getMerchantByUserId(userId);
        if (!"rejected".equals(merchant.getOnboardingStatus())) {
            throw new BadRequestException("Only rejected merchants can restart onboarding");
        }
        merchant.setOnboardingStatus("draft");
        merchant.setRejectionReason(null);
        merchantProfileRepository.save(merchant);
    }

    @Transactional
    public void acceptPgCommercials(String userId) {
        MerchantProfileEntity merchant = getMerchantByUserId(userId);
        merchant.setPgCommercialsAccepted(true);
        merchantProfileRepository.save(merchant);
    }

    @Transactional
    public void submitCpv(String userId, String cpvVideoPath) {
        MerchantProfileEntity merchant = getMerchantByUserId(userId);
        merchant.setCpvSubmitted(true);
        merchant.setCpvSubmittedAt(LocalDateTime.now());
        merchant.setCpvVideoPath(cpvVideoPath);
        merchant.setCpvStatus("submitted");
        merchantProfileRepository.save(merchant);
    }

    @Transactional
    public void signPgAgreement(String userId, SignAgreementRequest request, String ipAddress) {
        MerchantProfileEntity merchant = getMerchantByUserId(userId);
        merchant.setPgAgreementSigned(true);
        merchant.setAgreementSigned(true);
        merchant.setAgreementSignedAt(LocalDateTime.now());
        merchant.setAgreementIpAddress(ipAddress);
        if (request.getSignatureName() != null) merchant.setAgreementSignature(request.getSignatureName());
        String oldStatus = merchant.getOnboardingStatus();
        merchant.setOnboardingStatus("agreement_signed");
        merchantProfileRepository.save(merchant);
        auditLog(merchant.getId(), "AGREEMENT_SIGNED", oldStatus, "agreement_signed", userId, null);

        try {
            if (merchant.getEmail() != null && !merchant.getEmail().isBlank()) {
                String merchantName = merchant.getFullName() != null ? merchant.getFullName() : "Merchant";
                notificationService.sendEmail(merchant.getEmail(),
                        "SabbPe - Agreement Signed Successfully",
                        String.format("""
                                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                    <div style="background: #059669; padding: 24px; text-align: center;">
                                        <h1 style="color: white; margin: 0;">Agreement Signed</h1>
                                    </div>
                                    <div style="padding: 24px; background: #f9fafb;">
                                        <p>Dear %s,</p>
                                        <p>You have successfully signed the Payment Gateway agreement.</p>
                                        <p>Your application is now with the bank for final approval. You will receive an email once the bank completes its review.</p>
                                    </div>
                                </div>
                                """, merchantName));
            }
        } catch (Exception e) {
            log.warn("Failed to send agreement signed email: {}", e.getMessage());
        }
    }

    @Transactional
    public void confirmAgreementSigned(String userId) {
        MerchantProfileEntity merchant = getMerchantByUserId(userId);
        merchant.setAgreementSigned(true);
        merchant.setAgreementSignedAt(LocalDateTime.now());
        String oldStatus = merchant.getOnboardingStatus();
        merchant.setOnboardingStatus("agreement_signed");
        merchantProfileRepository.save(merchant);
        auditLog(merchant.getId(), "AGREEMENT_CONFIRMED", oldStatus, "agreement_signed", userId, null);

        try {
            if (merchant.getEmail() != null && !merchant.getEmail().isBlank()) {
                String merchantName = merchant.getFullName() != null ? merchant.getFullName() : "Merchant";
                notificationService.sendEmail(merchant.getEmail(),
                        "SabbPe - Agreement Signed Successfully",
                        String.format("""
                                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                    <div style="background: #059669; padding: 24px; text-align: center;">
                                        <h1 style="color: white; margin: 0;">Agreement Signed</h1>
                                    </div>
                                    <div style="padding: 24px; background: #f9fafb;">
                                        <p>Dear %s,</p>
                                        <p>You have successfully signed the Payment Gateway agreement.</p>
                                        <p>Your application is now with the bank for final approval.</p>
                                    </div>
                                </div>
                                """, merchantName));
            }
        } catch (Exception e) {
            log.warn("Failed to send agreement signed email: {}", e.getMessage());
        }
    }

    @Transactional
    public void saveSplitConfig(String userId, SplitConfigRequest request) {
        MerchantProfileEntity merchant = getMerchantByUserId(userId);
        if (request.getSplitSettlementEnabled() != null) merchant.setSplitSettlementEnabled(request.getSplitSettlementEnabled());
        if (request.getSplitPercentage() != null) merchant.setSplitPercentage(request.getSplitPercentage());
        if (request.getSplitAccountNumber() != null) merchant.setSplitAccountNumber(request.getSplitAccountNumber());
        if (request.getSplitIfscCode() != null) merchant.setSplitIfscCode(request.getSplitIfscCode());
        merchantProfileRepository.save(merchant);
    }

    private void saveKyc(String merchantId, MerchantProfileRequest.KycRequest request) {
        MerchantKycEntity kyc = kycRepository.findByMerchantId(merchantId).orElse(null);
        if (kyc == null) {
            kyc = new MerchantKycEntity();
            kyc.setId(UUID.randomUUID().toString());
            kyc.setMerchantId(merchantId);
        }
        if (request.getVideoKycCompleted() != null) kyc.setVideoKycCompleted(request.getVideoKycCompleted());
        if (request.getIsVideoCompleted() != null) kyc.setVideoKycCompleted(request.getIsVideoCompleted());
        if (request.getLocationCaptured() != null) kyc.setLocationCaptured(request.getLocationCaptured());
        if (request.getLocationVerified() != null) kyc.setLocationCaptured(request.getLocationVerified());
        if (request.getSelfieUrl() != null) kyc.setSelfieFilePath(request.getSelfieUrl());
        if (request.getLatitude() != null) kyc.setLatitude(BigDecimal.valueOf(request.getLatitude()));
        if (request.getLongitude() != null) kyc.setLongitude(BigDecimal.valueOf(request.getLongitude()));
        if (request.getFullAddress() != null) kyc.setFullAddress(request.getFullAddress());
        if (request.getArea() != null) kyc.setArea(request.getArea());
        if (request.getCity() != null) kyc.setCity(request.getCity());
        if (request.getState() != null) kyc.setState(request.getState());
        if (request.getPincode() != null) kyc.setPincode(request.getPincode());
        if (request.getCountry() != null) kyc.setCountry(request.getCountry());
        kyc.setKycStatus("pending");
        kycRepository.save(kyc);
    }

    private void saveDocuments(String merchantId, List<MerchantProfileRequest.DocumentRequest> docs) {
        for (MerchantProfileRequest.DocumentRequest doc : docs) {
            MerchantDocumentEntity entity = new MerchantDocumentEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setMerchantId(merchantId);
            entity.setDocumentType(doc.getDocumentType());
            entity.setFileName(doc.getFileName());
            entity.setFilePath(doc.getFilePath());
            entity.setFileSize(doc.getFileSize());
            entity.setMimeType(doc.getMimeType());
            entity.setDocCategory(doc.getDocCategory());
            entity.setStatus("uploaded");
            documentRepository.save(entity);
        }
    }

    private String maskString(String input) {
        if (input == null) return null;
        int len = input.length();
        if (len <= 4) return input;
        return input.substring(0, len - 4).replaceAll(".", "X") + input.substring(len - 4);
    }
}
