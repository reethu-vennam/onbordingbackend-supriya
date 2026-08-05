package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.model.MerchantBankDetailEntity;
import com.sabbpe.model.MerchantProfileEntity;
import com.sabbpe.model.OnboardingAuditLogEntity;
import com.sabbpe.repository.MerchantBankDetailRepository;
import com.sabbpe.repository.MerchantProfileRepository;
import com.sabbpe.repository.OnboardingAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/bank")
@RequiredArgsConstructor
public class BankStaffController {

    private final MerchantProfileRepository merchantProfileRepository;
    private final OnboardingAuditLogRepository auditLogRepository;
    private final MerchantBankDetailRepository bankDetailRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        log.info("Bank staff login attempt: {}", email);
        return ResponseEntity.ok(ApiResponse.success("Login successful", Map.of(
                "token", "bank_" + UUID.randomUUID(),
                "user", Map.of("email", email, "role", "bank_staff"))));
    }

    @GetMapping("/applications/pending")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingApplications() {
        List<MerchantProfileEntity> merchants = merchantProfileRepository
                .findByOnboardingStatus("pending_bank_approval");
        List<Map<String, Object>> apps = merchants.stream()
                .map(this::toApplicationMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(apps));
    }

    @GetMapping("/applications/agreement-signed")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAgreementSigned() {
        List<MerchantProfileEntity> merchants = merchantProfileRepository.findAll()
                .stream().filter(m -> "agreement_signed".equals(m.getOnboardingStatus()))
                .collect(Collectors.toList());
        List<Map<String, Object>> apps = merchants.stream()
                .map(this::toApplicationMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(apps));
    }

    @GetMapping("/applications/approved")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getApproved() {
        List<MerchantProfileEntity> merchants = merchantProfileRepository
                .findByOnboardingStatus("approved");
        List<Map<String, Object>> apps = merchants.stream()
                .map(this::toApplicationMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(apps));
    }

    @GetMapping("/applications/rejected")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRejected() {
        List<MerchantProfileEntity> merchants = merchantProfileRepository
                .findByOnboardingStatus("bank_rejected");
        List<Map<String, Object>> apps = merchants.stream()
                .map(this::toApplicationMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(apps));
    }

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApplication(
            @PathVariable String applicationId) {
        MerchantProfileEntity merchant = findByApplicationId(applicationId);
        if (merchant == null) {
            return ResponseEntity.ok(ApiResponse.error("NOT_FOUND", "Application not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(toApplicationDetailMap(merchant)));
    }

    @PostMapping("/applications/decide/{applicationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> decideApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> decision) {
        MerchantProfileEntity merchant = findByApplicationId(applicationId);
        if (merchant == null) {
            return ResponseEntity.ok(ApiResponse.error("NOT_FOUND", "Application not found"));
        }

        String status = decision.get("status") != null ? decision.get("status").toString() : "rejected";
        String notes = decision.get("notes") != null ? decision.get("notes").toString() : null;
        String merchantCode = decision.get("merchant_code") != null ? decision.get("merchant_code").toString() : null;

        String oldStatus = merchant.getOnboardingStatus();
        merchant.setDecisionAt(LocalDateTime.now());
        merchant.setBankDecisionNotes(notes);

        if ("approved".equals(status)) {
            merchant.setOnboardingStatus("approved");
            merchant.setBankApprovedAt(LocalDateTime.now());
            if (merchantCode != null)
                merchant.setBankMerchantCode(merchantCode);
            log.info("Bank APPROVED application {}", applicationId);
        } else {
            merchant.setOnboardingStatus("bank_rejected");
            merchant.setRejectionReason(notes);
            log.info("Bank REJECTED application {}: {}", applicationId, notes);
        }

        merchantProfileRepository.save(merchant);
        auditLog(merchant.getId(), "BANK_DECISION", oldStatus, merchant.getOnboardingStatus(), "bank_staff", notes);

        return ResponseEntity.ok(ApiResponse.success("Decision recorded", Map.of(
                "applicationId", applicationId,
                "newStatus", merchant.getOnboardingStatus())));
    }

    @PostMapping("/applications/send-agreement/{applicationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendAgreement(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> body) {
        MerchantProfileEntity merchant = findByApplicationId(applicationId);
        if (merchant == null) {
            return ResponseEntity.ok(ApiResponse.error("NOT_FOUND", "Application not found"));
        }

        String agreementText = body.get("agreement_text") != null ? body.get("agreement_text").toString() : null;
        String bankCommercials = body.get("bank_commercials") != null ? body.get("bank_commercials").toString() : null;

        merchant.setBankCommercials(bankCommercials);
        merchant.setBankCommercialsSetAt(LocalDateTime.now());
        merchant.setBankCommercialsSetBy("bank_staff");
        if (agreementText != null)
            merchant.setAgreementLink(agreementText);

        String oldStatus = merchant.getOnboardingStatus();
        merchant.setOnboardingStatus("agreement_pending");
        merchantProfileRepository.save(merchant);
        auditLog(merchant.getId(), "SEND_AGREEMENT", oldStatus, "agreement_pending",
                "bank_staff", "Agreement sent to merchant");

        log.info("Agreement sent for application {}", applicationId);
        return ResponseEntity.ok(ApiResponse.success("Agreement sent", Map.of(
                "applicationId", applicationId,
                "agreementLink", merchant.getAgreementLink())));
    }

    @PostMapping("/applications/send-agreement-link/{applicationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendAgreementLink(
            @PathVariable String applicationId,
            @RequestBody Map<String, String> body) {
        MerchantProfileEntity merchant = findByApplicationId(applicationId);
        if (merchant == null) {
            return ResponseEntity.ok(ApiResponse.error("NOT_FOUND", "Application not found"));
        }

        String link = body.get("agreement_link");
        merchant.setAgreementLink(link);
        String oldStatus = merchant.getOnboardingStatus();
        merchant.setOnboardingStatus("agreement_pending");
        merchantProfileRepository.save(merchant);
        auditLog(merchant.getId(), "SEND_AGREEMENT_LINK", oldStatus, "agreement_pending", "bank_staff", null);

        log.info("Agreement link sent for application {}: {}", applicationId, link);
        return ResponseEntity.ok(ApiResponse.success("Agreement link sent", Map.of(
                "applicationId", applicationId,
                "agreementLink", link)));
    }

    @PostMapping("/applications/final-decision/{applicationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> finalDecision(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> decision) {
        MerchantProfileEntity merchant = findByApplicationId(applicationId);
        if (merchant == null) {
            return ResponseEntity.ok(ApiResponse.error("NOT_FOUND", "Application not found"));
        }

        if (!Boolean.TRUE.equals(merchant.getAgreementSigned())) {
            return ResponseEntity
                    .ok(ApiResponse.error("AGREEMENT_NOT_SIGNED", "Merchant has not signed the agreement yet"));
        }

        String status = decision.get("status") != null ? decision.get("status").toString() : "approved";
        String notes = decision.get("notes") != null ? decision.get("notes").toString() : null;
        String merchantCode = decision.get("merchant_code") != null ? decision.get("merchant_code").toString() : null;

        String oldStatus = merchant.getOnboardingStatus();
        merchant.setDecisionAt(LocalDateTime.now());
        merchant.setBankDecisionNotes(notes);

        if ("approved".equals(status)) {
            merchant.setOnboardingStatus("approved");
            merchant.setBankApprovedAt(LocalDateTime.now());
            if (merchantCode != null)
                merchant.setBankMerchantCode(merchantCode);
            log.info("FINAL APPROVE application {}", applicationId);
        } else {
            merchant.setOnboardingStatus("bank_rejected");
            merchant.setRejectionReason(notes);
            log.info("FINAL REJECT application {}: {}", applicationId, notes);
        }

        merchantProfileRepository.save(merchant);
        auditLog(merchant.getId(), "FINAL_DECISION", oldStatus, merchant.getOnboardingStatus(), "bank_staff", notes);

        return ResponseEntity.ok(ApiResponse.success("Final decision recorded", Map.of(
                "applicationId", applicationId,
                "newStatus", merchant.getOnboardingStatus())));
    }

    private MerchantProfileEntity findByApplicationId(String applicationId) {
        MerchantProfileEntity merchant = merchantProfileRepository
                .findByBankApplicationId(applicationId).orElse(null);
        if (merchant == null) {
            merchant = merchantProfileRepository.findById(applicationId).orElse(null);
        }
        return merchant;
    }

    private Map<String, Object> toApplicationMap(MerchantProfileEntity m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("user_id", m.getUserId());
        map.put("full_name", m.getFullName());
        map.put("business_name", m.getBusinessName() != null ? m.getBusinessName() : "");
        map.put("email", m.getEmail());
        map.put("mobile_number", m.getMobileNumber());
        map.put("onboarding_status", m.getOnboardingStatus());
        map.put("cpv_status", m.getCpvStatus());
        map.put("agreement_signed", m.getAgreementSigned());
        map.put("application_id", m.getApplicationId());
        map.put("bank_application_id", m.getBankApplicationId());
        map.put("bank_merchant_code", m.getBankMerchantCode());
        map.put("has_pg_product", m.getHasPgProduct());
        map.put("selected_products", m.getSelectedProducts());
        map.put("bank_commercials", parseJson(m.getBankCommercials()));
        map.put("pg_agreement_signed", m.getPgAgreementSigned());
        map.put("pg_agreement_signed_at", m.getPgAgreementSignedAt());
        map.put("created_at", m.getCreatedAt());
        map.put("updated_at", m.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toApplicationDetailMap(MerchantProfileEntity m) {
        Map<String, Object> map = toApplicationMap(m);
        map.put("pan_number", m.getPanNumber());
        map.put("gst_number", m.getGstNumber());
        map.put("entity_type", m.getEntityType());
        map.put("aadhaar_number", m.getAadhaarNumber());
        map.put("bank_decision_notes", m.getBankDecisionNotes());
        map.put("decision_at", m.getDecisionAt());
        map.put("bank_approved_at", m.getBankApprovedAt());
        map.put("agreement_link", m.getAgreementLink());
        map.put("pg_agreement_signature", m.getPgAgreementSignature());
        map.put("cpv_video_path", m.getCpvVideoPath());
        map.put("cpv_submitted_at", m.getCpvSubmittedAt());
        map.put("cpv_verified_at", m.getCpvVerifiedAt());
        map.put("rejection_reason", m.getRejectionReason());
        map.put("total_monthly_cost", m.getTotalMonthlyCost());
        map.put("total_onetime_cost", m.getTotalOnetimeCost());

        try {
            Optional<MerchantBankDetailEntity> optBank = bankDetailRepository.findByMerchantId(m.getId());
            if (optBank.isPresent()) {
                String json = optBank.get().getBankDetailsJson();
                if (json != null && !json.isBlank()) {
                    List<Map<String, Object>> bankList = objectMapper.readValue(json,
                            new TypeReference<List<Map<String, Object>>>() {});
                    map.put("bank_details", bankList);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load bank_details for merchant {}: {}", m.getId(), e.getMessage());
        }

        return map;
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON column: {}", e.getMessage());
            return null;
        }
    }

    private void auditLog(String merchantId, String action, String oldStatus, String newStatus, String performedBy,
            String notes) {
        try {
            OnboardingAuditLogEntity audit = new OnboardingAuditLogEntity();
            audit.setMerchantId(merchantId);
            audit.setAction(action);
            audit.setPreviousStatus(oldStatus);
            audit.setNewStatus(newStatus);
            audit.setPerformedBy(performedBy);
            audit.setNotes(notes);
            auditLogRepository.save(audit);
        } catch (Exception e) {
            log.warn("Failed to write audit log for merchant {}: {}", merchantId, e.getMessage());
        }
    }
}
