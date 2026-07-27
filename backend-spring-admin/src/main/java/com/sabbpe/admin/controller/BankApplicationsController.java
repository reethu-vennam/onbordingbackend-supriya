package com.sabbpe.admin.controller;

import com.sabbpe.admin.model.MerchantProfileEntity;
import com.sabbpe.admin.repository.MerchantProfileRepository;
import com.sabbpe.admin.service.NotificationService;
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
public class BankApplicationsController {

    private final MerchantProfileRepository merchantProfileRepository;
    private final NotificationService notificationService;

    @GetMapping("/applications/pending")
    public ResponseEntity<?> getPendingApplications() {
        List<MerchantProfileEntity> merchants = merchantProfileRepository
                .findByOnboardingStatus("pending_bank_approval");
        List<Map<String, Object>> apps = merchants.stream()
                .map(this::toApplicationMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("data", apps));
    }

    @GetMapping("/applications/agreement-signed")
    public ResponseEntity<?> getAgreementSignedApplications() {
        List<MerchantProfileEntity> merchants = merchantProfileRepository.findAll()
                .stream()
                .filter(m -> "agreement_signed".equals(m.getOnboardingStatus()))
                .collect(Collectors.toList());
        List<Map<String, Object>> apps = merchants.stream()
                .map(this::toApplicationMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("data", apps));
    }

    @GetMapping("/applications/approved")
    public ResponseEntity<?> getApprovedApplications() {
        List<MerchantProfileEntity> merchants = merchantProfileRepository
                .findByOnboardingStatus("approved");
        List<Map<String, Object>> apps = merchants.stream()
                .map(this::toApplicationMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("data", apps));
    }

    @GetMapping("/applications/rejected")
    public ResponseEntity<?> getRejectedApplications() {
        List<MerchantProfileEntity> merchants = merchantProfileRepository
                .findByOnboardingStatus("bank_rejected");
        List<Map<String, Object>> apps = merchants.stream()
                .map(this::toApplicationMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("data", apps));
    }

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<?> getApplication(@PathVariable String applicationId) {
        MerchantProfileEntity merchant = findByApplicationId(applicationId);
        if (merchant == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Application not found"));
        }
        return ResponseEntity.ok(Map.of("data", toApplicationDetailMap(merchant)));
    }

    @PostMapping("/applications/decide/{applicationId}")
    public ResponseEntity<?> decideApplication(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> body) {
        MerchantProfileEntity merchant = findByApplicationId(applicationId);
        if (merchant == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Application not found"));
        }

        String status = body.get("status") != null ? body.get("status").toString() : "rejected";
        String notes = body.get("notes") != null ? body.get("notes").toString() : null;
        String merchantCode = body.get("merchant_code") != null ? body.get("merchant_code").toString() : null;

        String oldStatus = merchant.getOnboardingStatus();
        merchant.setDecisionAt(LocalDateTime.now());
        merchant.setBankDecisionNotes(notes);

        if ("approved".equals(status)) {
            merchant.setOnboardingStatus("approved");
            merchant.setBankApprovedAt(LocalDateTime.now());
            if (merchantCode != null) merchant.setBankMerchantCode(merchantCode);
            log.info("Bank APPROVED application {}", applicationId);
        } else {
            merchant.setOnboardingStatus("bank_rejected");
            merchant.setRejectionReason(notes);
            log.info("Bank REJECTED application {}: {}", applicationId, notes);
        }

        merchantProfileRepository.save(merchant);

        sendMerchantEmail(merchant, "approved".equals(status) ? "approved" : "rejected", notes);

        return ResponseEntity.ok(Map.of(
                "data", Map.of(
                        "applicationId", applicationId,
                        "newStatus", merchant.getOnboardingStatus())));
    }

    @PostMapping("/applications/send-agreement/{applicationId}")
    public ResponseEntity<?> sendAgreement(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> body) {
        MerchantProfileEntity merchant = findByApplicationId(applicationId);
        if (merchant == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Application not found"));
        }

        String agreementText = body.get("agreement_text") != null ? body.get("agreement_text").toString() : null;
        String bankCommercials = body.get("bank_commercials") != null ? body.get("bank_commercials").toString() : null;

        merchant.setBankCommercials(bankCommercials);
        merchant.setBankCommercialsSetAt(LocalDateTime.now());
        merchant.setBankCommercialsSetBy("bank_staff");
        if (agreementText != null) merchant.setAgreementLink(agreementText);
        merchant.setOnboardingStatus("agreement_pending");

        merchantProfileRepository.save(merchant);
        log.info("Agreement sent for application {}", applicationId);

        try {
            if (merchant.getEmail() != null && !merchant.getEmail().isBlank()) {
                String merchantName = merchant.getFullName() != null ? merchant.getFullName() : "Merchant";
                notificationService.sendEmail(merchant.getEmail(),
                        "SabbPe - Agreement Ready for Signing",
                        String.format("""
                                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                    <div style="background: #4f46e5; padding: 24px; text-align: center;">
                                        <h1 style="color: white; margin: 0;">Agreement Ready</h1>
                                    </div>
                                    <div style="padding: 24px; background: #f9fafb;">
                                        <p>Dear %s,</p>
                                        <p>The bank has sent your merchant agreement with commercial rates.</p>
                                        <p>Please review and sign the agreement to proceed with your onboarding.</p>
                                        <p>Log in to your dashboard to view and sign the agreement.</p>
                                    </div>
                                </div>
                                """, merchantName));
            }
        } catch (Exception e) {
            log.warn("Failed to send agreement email: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "data", Map.of(
                        "applicationId", applicationId,
                        "agreementLink", merchant.getAgreementLink())));
    }

    @PostMapping("/applications/send-agreement-link/{applicationId}")
    public ResponseEntity<?> sendAgreementLink(
            @PathVariable String applicationId,
            @RequestBody Map<String, String> body) {
        MerchantProfileEntity merchant = findByApplicationId(applicationId);
        if (merchant == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Application not found"));
        }

        String link = body.get("link") != null ? body.get("link") : body.get("agreement_link");
        merchant.setAgreementLink(link);
        merchant.setOnboardingStatus("agreement_pending");
        merchantProfileRepository.save(merchant);

        log.info("Agreement link sent for application {}: {}", applicationId, link);
        return ResponseEntity.ok(Map.of(
                "data", Map.of(
                        "applicationId", applicationId,
                        "agreementLink", link)));
    }

    @PostMapping("/applications/final-decision/{applicationId}")
    public ResponseEntity<?> finalDecision(
            @PathVariable String applicationId,
            @RequestBody Map<String, Object> body) {
        MerchantProfileEntity merchant = findByApplicationId(applicationId);
        if (merchant == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Application not found"));
        }

        String status = body.get("status") != null ? body.get("status").toString() : "approved";
        String notes = body.get("notes") != null ? body.get("notes").toString() : null;
        String merchantCode = body.get("merchant_code") != null ? body.get("merchant_code").toString() : null;

        String oldStatus = merchant.getOnboardingStatus();
        merchant.setDecisionAt(LocalDateTime.now());
        merchant.setBankDecisionNotes(notes);
        boolean finalApprovalArchiveEmailSent = false;
        String finalApprovalArchiveEmailError = null;

        if ("approved".equals(status)) {
            merchant.setOnboardingStatus("approved");
            merchant.setBankApprovedAt(LocalDateTime.now());
            if (merchantCode != null) merchant.setBankMerchantCode(merchantCode);
            log.info("FINAL APPROVE application {}", applicationId);

            sendMerchantEmail(merchant, "approved", notes);

            try {
                sendAdminApprovalEmail(merchant);
                finalApprovalArchiveEmailSent = true;
            } catch (Exception e) {
                finalApprovalArchiveEmailError = e.getMessage();
                log.warn("Failed to send admin approval email: {}", e.getMessage());
            }
        } else {
            merchant.setOnboardingStatus("bank_rejected");
            merchant.setRejectionReason(notes);
            log.info("FINAL REJECT application {}: {}", applicationId, notes);

            sendMerchantEmail(merchant, "rejected", notes);
        }

        merchantProfileRepository.save(merchant);

        Map<String, Object> responseData = new LinkedHashMap<>();
        responseData.put("applicationId", applicationId);
        responseData.put("newStatus", merchant.getOnboardingStatus());
        responseData.put("finalApprovalArchiveEmailSent", finalApprovalArchiveEmailSent);
        if (finalApprovalArchiveEmailError != null) {
            responseData.put("finalApprovalArchiveEmailError", finalApprovalArchiveEmailError);
        }

        return ResponseEntity.ok(Map.of("data", responseData));
    }

    private void sendAdminApprovalEmail(MerchantProfileEntity merchant) {
        String adminEmail = "vendor.onboarding@sabbpe.com";
        String merchantName = merchant.getFullName() != null ? merchant.getFullName() : "N/A";
        String businessName = merchant.getBusinessName() != null ? merchant.getBusinessName() : "N/A";
        String email = merchant.getEmail() != null ? merchant.getEmail() : "N/A";
        String pan = merchant.getPanNumber() != null ? merchant.getPanNumber() : "N/A";
        String gst = merchant.getGstNumber() != null ? merchant.getGstNumber() : "N/A";
        String mobile = merchant.getMobileNumber() != null ? merchant.getMobileNumber() : "N/A";
        String entityType = merchant.getEntityType() != null ? merchant.getEntityType() : "N/A";
        String merchantCode = merchant.getBankMerchantCode() != null ? merchant.getBankMerchantCode() : "Pending";
        String bankNotes = merchant.getBankDecisionNotes() != null ? merchant.getBankDecisionNotes() : "N/A";

        notificationService.sendEmail(adminEmail,
                "SabbPe - Merchant Approved: " + businessName,
                String.format("""
                        <div style="font-family: Arial, sans-serif; max-width: 700px; margin: 0 auto;">
                            <div style="background: #059669; padding: 24px; text-align: center;">
                                <h1 style="color: white; margin: 0;">Merchant Approved</h1>
                                <p style="color: #d1fae5; margin: 4px 0 0;">Bank Final Approval Complete</p>
                            </div>
                            <div style="padding: 24px; background: #f9fafb;">
                                <p>The following merchant application has been <b>approved</b> by the bank:</p>
                                <table style="width: 100%%; border-collapse: collapse; margin: 16px 0;">
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: 600; width: 180px;">Merchant Name</td><td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">%s</td></tr>
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: 600;">Business Name</td><td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">%s</td></tr>
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: 600;">Email</td><td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">%s</td></tr>
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: 600;">Mobile</td><td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">%s</td></tr>
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: 600;">PAN</td><td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">%s</td></tr>
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: 600;">GST</td><td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">%s</td></tr>
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: 600;">Entity Type</td><td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">%s</td></tr>
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: 600;">Merchant Code</td><td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">%s</td></tr>
                                    <tr><td style="padding: 8px; border-bottom: 1px solid #e5e7eb; font-weight: 600;">Bank Notes</td><td style="padding: 8px; border-bottom: 1px solid #e5e7eb;">%s</td></tr>
                                </table>
                                <p style="color: #6b7280; font-size: 13px;">This merchant is now active and can start accepting payments through SabbPe.</p>
                            </div>
                        </div>
                        """, merchantName, businessName, email, mobile, pan, gst, entityType, merchantCode, bankNotes));
    }

    private void sendMerchantEmail(MerchantProfileEntity merchant, String status, String notes) {
        try {
            if (merchant.getEmail() == null || merchant.getEmail().isBlank()) return;
            String merchantName = merchant.getFullName() != null ? merchant.getFullName() : "Merchant";

            if ("approved".equals(status)) {
                notificationService.sendEmail(merchant.getEmail(),
                        "Congratulations! Your SabbPe Account Has Been Approved",
                        String.format("""
                                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                    <div style="background: #059669; padding: 24px; text-align: center;">
                                        <h1 style="color: white; margin: 0;">Account Approved!</h1>
                                    </div>
                                    <div style="padding: 24px; background: #f9fafb;">
                                        <p>Dear %s,</p>
                                        <p>Congratulations! Your merchant account has been <b>approved</b> by the bank.</p>
                                        <p>You can now start accepting payments through SabbPe.</p>
                                        <p>Your account is now active. Log in to your dashboard to get started.</p>
                                    </div>
                                </div>
                                """, merchantName));
            } else {
                String reasonText = notes != null ? notes : "Please contact support for details.";
                notificationService.sendEmail(merchant.getEmail(),
                        "SabbPe - Application Update",
                        String.format("""
                                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                    <div style="background: #dc2626; padding: 24px; text-align: center;">
                                        <h1 style="color: white; margin: 0;">Application Update</h1>
                                    </div>
                                    <div style="padding: 24px; background: #f9fafb;">
                                        <p>Dear %s,</p>
                                        <p>We regret to inform you that your application has been <b>rejected</b> by the bank.</p>
                                        <p><b>Reason:</b> %s</p>
                                        <p>Please contact our support team for assistance.</p>
                                    </div>
                                </div>
                                """, merchantName, reasonText));
            }
        } catch (Exception e) {
            log.warn("Failed to send bank decision email: {}", e.getMessage());
        }
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
        map.put("id", m.getBankApplicationId() != null ? m.getBankApplicationId() : m.getId());
        map.put("merchantId", m.getId());
        map.put("full_name", m.getFullName());
        map.put("email", m.getEmail());
        map.put("business_name", m.getBusinessName() != null ? m.getBusinessName() : "");
        map.put("onboarding_status", m.getOnboardingStatus());
        map.put("status", m.getOnboardingStatus());
        map.put("agreement_signed", m.getAgreementSigned());
        map.put("created_at", m.getCreatedAt());
        map.put("updated_at", m.getUpdatedAt());
        map.put("bank_merchant_code", m.getBankMerchantCode());
        return map;
    }

    private Map<String, Object> toApplicationDetailMap(MerchantProfileEntity m) {
        Map<String, Object> map = toApplicationMap(m);
        map.put("pan_number", m.getPanNumber());
        map.put("gst_number", m.getGstNumber());
        map.put("entity_type", m.getEntityType());
        map.put("mobile_number", m.getMobileNumber());
        map.put("cpv_status", m.getCpvStatus());
        map.put("cpv_video_path", m.getCpvVideoPath());
        map.put("rejection_reason", m.getRejectionReason());
        map.put("bank_decision_notes", m.getBankDecisionNotes());
        map.put("agreement_link", m.getAgreementLink());
        map.put("bank_commercials", m.getBankCommercials());
        map.put("total_monthly_cost", m.getTotalMonthlyCost());
        map.put("total_onetime_cost", m.getTotalOnetimeCost());
        map.put("selected_products", m.getSelectedProducts());
        return map;
    }
}
