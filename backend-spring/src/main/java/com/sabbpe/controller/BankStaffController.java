package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.model.MerchantProfileEntity;
import com.sabbpe.model.OnboardingAuditLogEntity;
import com.sabbpe.repository.MerchantProfileRepository;
import com.sabbpe.repository.OnboardingAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        merchantProfileRepository.save(merchant);
        auditLog(merchant.getId(), "SEND_AGREEMENT", merchant.getOnboardingStatus(), merchant.getOnboardingStatus(),
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

        String link = body.get("link");
        merchant.setAgreementLink(link);
        merchantProfileRepository.save(merchant);

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
        map.put("merchantId", m.getId());
        map.put("userId", m.getUserId());
        map.put("fullName", m.getFullName());
        map.put("businessName", m.getBusinessName() != null ? m.getBusinessName() : "");
        map.put("email", m.getEmail());
        map.put("mobileNumber", m.getMobileNumber());
        map.put("status", m.getOnboardingStatus());
        map.put("cpvStatus", m.getCpvStatus());
        map.put("agreementSigned", m.getAgreementSigned());
        map.put("bankApplicationId", m.getBankApplicationId());
        map.put("bankMerchantCode", m.getBankMerchantCode());
        map.put("createdAt", m.getCreatedAt());
        return map;
    }

    private Map<String, Object> toApplicationDetailMap(MerchantProfileEntity m) {
        Map<String, Object> map = toApplicationMap(m);
        map.put("panNumber", m.getPanNumber());
        map.put("gstNumber", m.getGstNumber());
        map.put("entityType", m.getEntityType());
        map.put("bankDecisionNotes", m.getBankDecisionNotes());
        map.put("decisionAt", m.getDecisionAt());
        map.put("bankApprovedAt", m.getBankApprovedAt());
        map.put("agreementLink", m.getAgreementLink());
        map.put("bankCommercials", m.getBankCommercials());
        map.put("cpvVideoPath", m.getCpvVideoPath());
        map.put("cpvSubmittedAt", m.getCpvSubmittedAt());
        map.put("cpvVerifiedAt", m.getCpvVerifiedAt());
        map.put("rejectionReason", m.getRejectionReason());
        map.put("totalMonthlyCost", m.getTotalMonthlyCost());
        map.put("totalOnetimeCost", m.getTotalOnetimeCost());
        map.put("selectedProducts", m.getSelectedProducts());
        return map;
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
