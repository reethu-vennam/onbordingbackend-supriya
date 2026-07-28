package com.sabbpe.admin.service;

import com.sabbpe.admin.exception.BadRequestException;
import com.sabbpe.admin.exception.ForbiddenException;
import com.sabbpe.admin.exception.ResourceNotFoundException;
import com.sabbpe.admin.model.*;
import com.sabbpe.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantKycRepository kycRepository;
    private final MerchantDocumentRepository documentRepository;
    private final MerchantPersonRepository personRepository;
    private final MerchantBankDetailRepository bankDetailRepository;
    private final SupportKycActionRepository supportKycActionRepository;
    private final NotificationService notificationService;

    private static final List<String> ALLOWED_MODULES = List.of(
            "merchant_onboarding", "payments", "authentication", "inventory",
            "orders", "reports", "kyc", "settlement", "customer_portal");
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.ofEntries(
            Map.entry("open", Set.of("assigned")),
            Map.entry("assigned", Set.of("in_progress")),
            Map.entry("in_progress", Set.of("resolved", "waiting_customer")),
            Map.entry("waiting_customer", Set.of("in_progress", "resolved", "closed")),
            Map.entry("resolved", Set.of("closed")),
            Map.entry("closed", Set.of()));
    private static final Set<String> ADMIN_ROLES = Set.of("super_admin", "admin", "admin_role");

    public Map<String, Object> createTicket(String module, String referenceId, String title,
                                            String description, String priority, String userId) {
        if (!ALLOWED_MODULES.contains(module)) throw new BadRequestException("Invalid module: " + module);
        TicketEntity ticket = TicketEntity.builder()
                .module(module).referenceId(referenceId).title(title).description(description)
                .priority(priority != null ? priority : "medium").status("open").createdBy(userId).build();
        return toMap(ticketRepository.save(ticket));
    }

    public Map<String, Object> createMerchantTicket(String merchantId, String title, String description) {
        TicketEntity ticket = TicketEntity.builder()
                .module("merchant_onboarding").title(title).description(description)
                .priority("medium").status("open").createdBy(merchantId).build();
        ticket = ticketRepository.save(ticket);
        final TicketEntity saved = ticket;
        merchantProfileRepository.findByUserId(merchantId).ifPresent(m -> {
            if (m.getEmail() != null && !m.getEmail().isBlank()) {
                notificationService.sendEmail(m.getEmail(), "Your Support Ticket Has Been Created - SabbPe",
                        "<p>Your ticket <b>" + saved.getTitle() + "</b> has been created.</p>");
            }
        });
        return toMap(ticket);
    }

    public Map<String, Object> getTickets(String userId, String userRole,
                                          String status, String module, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit);
        boolean isAdmin = ADMIN_ROLES.contains(userRole);
        Page<TicketEntity> ticketPage;
        if (isAdmin) {
            if (status != null) ticketPage = ticketRepository.findByStatusOrderByCreatedAtDesc(status, pageRequest);
            else if (module != null) ticketPage = ticketRepository.findByModuleOrderByCreatedAtDesc(module, pageRequest);
            else ticketPage = ticketRepository.findAll(pageRequest);
        } else {
            if (status != null) ticketPage = ticketRepository.findByAssignedToAndStatusOrderByCreatedAtDesc(userId, status, pageRequest);
            else ticketPage = ticketRepository.findByAssignedToOrderByCreatedAtDesc(userId, pageRequest);
        }
        List<Map<String, Object>> tickets = ticketPage.getContent().stream()
                .map(this::toMapWithDetails).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("total", ticketPage.getTotalElements());
        result.put("page", page);
        result.put("pages", ticketPage.getTotalPages());
        result.put("tickets", tickets);
        return result;
    }

    public Map<String, Object> assignTicket(String ticketId, String assignToUserId) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        ticket.setAssignedTo(assignToUserId);
        ticket.setStatus("assigned");
        return toMap(ticketRepository.save(ticket));
    }

    public Map<String, Object> updateTicketStatus(String ticketId, String newStatus, String userRole) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(ticket.getStatus(), Set.of());
        if (!allowed.contains(newStatus))
            throw new BadRequestException("Invalid transition from " + ticket.getStatus() + " to " + newStatus);
        if ("in_progress".equals(newStatus) && !"support".equals(userRole))
            throw new ForbiddenException("Only support can start work");
        if ("resolved".equals(newStatus) && !"support".equals(userRole))
            throw new ForbiddenException("Only support can resolve");
        if ("closed".equals(newStatus) && !ADMIN_ROLES.contains(userRole))
            throw new ForbiddenException("Only admin can close ticket");
        ticket.setStatus(newStatus);
        return toMap(ticketRepository.save(ticket));
    }

    public Map<String, Object> getTicketStats() {
        List<Object[]> statusCounts = ticketRepository.countByStatusGrouped();
        Map<String, Long> countMap = new HashMap<>();
        long total = 0;
        for (Object[] row : statusCounts) {
            String s = (String) row[0];
            Long c = (Long) row[1];
            countMap.put(s, c);
            total += c;
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("open", countMap.getOrDefault("open", 0L));
        stats.put("assigned", countMap.getOrDefault("assigned", 0L));
        stats.put("in_progress", countMap.getOrDefault("in_progress", 0L));
        stats.put("resolved", countMap.getOrDefault("resolved", 0L));
        stats.put("waiting_customer", countMap.getOrDefault("waiting_customer", 0L));
        stats.put("closed", countMap.getOrDefault("closed", 0L));
        return stats;
    }

    public List<Map<String, Object>> getMerchantTickets(String merchantId) {
        return ticketRepository.findByCreatedByAndModuleOrderByCreatedAtDesc(merchantId, "merchant_onboarding").stream()
                .map(this::toMap).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMerchantTicketMessages(String ticketId, String merchantId) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        if (!ticket.getCreatedBy().equals(merchantId)) throw new ForbiddenException("Access denied");
        return ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream().map(this::toMessageMap).collect(Collectors.toList());
    }

    public Map<String, Object> sendMerchantTicketMessage(String ticketId, String merchantId, String message) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        if (!ticket.getCreatedBy().equals(merchantId)) throw new ForbiddenException("Access denied");
        TicketMessageEntity msg = TicketMessageEntity.builder()
                .ticketId(ticketId).senderId(merchantId).senderRole("merchant").message(message).build();
        return toMessageMap(ticketMessageRepository.save(msg));
    }

    public List<Map<String, Object>> getTicketMessages(String ticketId) {
        return ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream().map(this::toMessageMap).collect(Collectors.toList());
    }

    public Map<String, Object> sendTicketMessage(String ticketId, String senderId, String senderRole, String message) {
        TicketMessageEntity msg = TicketMessageEntity.builder()
                .ticketId(ticketId).senderId(senderId).senderRole(senderRole).message(message).build();
        return toMessageMap(ticketMessageRepository.save(msg));
    }

    @Value("${app.main-backend-url:http://localhost:8080}")
    private String mainBackendUrl;

    private String buildPublicUrl(String filePath) {
        return mainBackendUrl + (filePath.startsWith("/uploads/") ? filePath : "/uploads/" + filePath);
    }

    public Map<String, Object> getMerchantReviewData(String merchantId, String userRole, String userId) {
        MerchantProfileEntity profile = merchantProfileRepository.findByUserId(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", merchantId));
        List<MerchantDocumentEntity> documents = documentRepository.findByMerchantId(profile.getId());
        List<Map<String, Object>> docList = documents.stream().map(d -> {
            Map<String, Object> dm = new HashMap<>();
            dm.put("id", d.getId());
            dm.put("merchant_id", d.getMerchantId());
            dm.put("document_type", d.getDocumentType());
            dm.put("file_name", d.getFileName());
            dm.put("file_path", d.getFilePath());
            dm.put("status", d.getStatus());
            dm.put("rejection_reason", d.getRejectionReason());
            dm.put("uploaded_at", d.getUploadedAt());
            dm.put("verified_at", d.getVerifiedAt());
            if (d.getFilePath() != null && !d.getFilePath().isBlank()) {
                dm.put("public_url", buildPublicUrl(d.getFilePath()));
            }
            return dm;
        }).collect(Collectors.toList());

        Map<String, Object> profileMap = toMap(profile);

        try {
            bankDetailRepository.findByMerchantId(profile.getId()).ifPresent(bank -> {
                Map<String, Object> bm = new HashMap<>();
                bm.put("id", bank.getId());
                bm.put("account_number", bank.getAccountNumber());
                bm.put("ifsc_code", bank.getIfscCode());
                bm.put("bank_name", bank.getBankName());
                bm.put("account_holder_name", bank.getAccountHolderName());
                profileMap.put("bank_details", bm);
            });
        } catch (Exception e) {
            log.debug("Bank details not available for merchant {}", profile.getId());
        }

        try {
            kycRepository.findByMerchantId(profile.getId()).ifPresent(kyc -> {
                Map<String, Object> km = new HashMap<>();
                km.put("id", kyc.getId());
                km.put("video_kyc_completed", kyc.getVideoKycCompleted());
                km.put("location_captured", kyc.getLocationCaptured());
                km.put("latitude", kyc.getLatitude());
                km.put("longitude", kyc.getLongitude());
                km.put("kyc_status", kyc.getKycStatus());
                km.put("rejection_reason", kyc.getRejectionReason());
                km.put("full_address", kyc.getFullAddress());
                km.put("city", kyc.getCity());
                km.put("state", kyc.getState());
                km.put("pincode", kyc.getPincode());
                km.put("video_kyc_file_path", kyc.getVideoKycFilePath());
                km.put("selfie_file_path", kyc.getSelfieFilePath());
                profileMap.put("kyc", List.of(km));
            });
        } catch (Exception e) {
            log.debug("KYC not available for merchant {}", profile.getId());
        }

        try {
            List<Map<String, Object>> personList = personRepository.findByMerchantIdOrderBySequenceOrderAsc(profile.getId())
                    .stream().map(p -> {
                        Map<String, Object> pm = new HashMap<>();
                        pm.put("id", p.getId());
                        pm.put("role", p.getRole());
                        pm.put("full_name", p.getFullName());
                        pm.put("pan_number", p.getPanNumber());
                        pm.put("is_authorized_signatory", p.getIsAuthorizedSignatory());
                        pm.put("sequence_order", p.getSequenceOrder());
                        return pm;
                    }).collect(Collectors.toList());
            profileMap.put("persons", personList);
        } catch (Exception e) {
            log.debug("Persons not available for merchant {}", profile.getId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("profile", profileMap);
        result.put("documents", docList);
        return result;
    }

    @Transactional
    public Map<String, Object> reviewMerchant(String merchantId, String status, String reviewNotes, String performedBy) {
        if (!"approved".equals(status) && !"rejected".equals(status))
            throw new BadRequestException("Invalid request data");
        MerchantProfileEntity profile = merchantProfileRepository.findByUserId(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", merchantId));
        if ("rejected".equals(status)) {
            profile.setOnboardingStatus("rejected");
            profile.setRejectionReason(reviewNotes);
        } else {
            profile.setOnboardingStatus("cpv_pending");
            profile.setCpvStatus("pending");
            profile.setRejectionReason(null);
        }
        profile.setReviewedBy(performedBy);
        profile.setReviewedAt(LocalDateTime.now());
        merchantProfileRepository.save(profile);
        String kycStatus = "approved".equals(status) ? "verified" : "rejected";
        kycRepository.findByMerchantId(profile.getId()).ifPresent(kyc -> {
            kyc.setKycStatus(kycStatus);
            kyc.setRejectionReason(reviewNotes);
            kyc.setVerifiedBy(performedBy);
            if ("approved".equals(status)) kyc.setVerifiedAt(LocalDateTime.now());
            kycRepository.save(kyc);
        });
        List<TicketEntity> tickets = ticketRepository.findByCreatedByAndModuleOrderByCreatedAtDesc(profile.getUserId(), "merchant_onboarding");
        if (!tickets.isEmpty()) {
            TicketEntity ticket = tickets.get(0);
            ticket.setStatus("approved".equals(status) ? "in_progress" : "waiting_customer");
            ticketRepository.save(ticket);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "approved".equals(status) ? "Merchant approved, sent to CPV" : "Merchant rejected");
        result.put("newStatus", "approved".equals(status) ? "cpv_pending" : "rejected");

        try {
            if (profile.getEmail() != null && !profile.getEmail().isBlank()) {
                String merchantName = profile.getFullName() != null ? profile.getFullName() : "Merchant";
                if ("approved".equals(status)) {
                    notificationService.sendEmail(profile.getEmail(),
                            "SabbPe - KYC Verified, Shop Verification Required",
                            String.format("""
                                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                        <div style="background: #7c3aed; padding: 24px; text-align: center;">
                                            <h1 style="color: white; margin: 0;">KYC Verified</h1>
                                        </div>
                                        <div style="padding: 24px; background: #f9fafb;">
                                            <p>Dear %s,</p>
                                            <p>Your KYC documents have been verified successfully.</p>
                                            <p>Please record a short video of your business premises (CPV) to continue.</p>
                                            <p>Log in to your dashboard to record the video.</p>
                                        </div>
                                    </div>
                                    """, merchantName));
                } else {
                    String reasonText = reviewNotes != null ? reviewNotes : "Please contact support.";
                    notificationService.sendEmail(profile.getEmail(),
                            "SabbPe - Application Update",
                            String.format("""
                                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                        <div style="background: #dc2626; padding: 24px; text-align: center;">
                                            <h1 style="color: white; margin: 0;">Application Update</h1>
                                        </div>
                                        <div style="padding: 24px; background: #f9fafb;">
                                            <p>Dear %s,</p>
                                            <p>We regret to inform you that your application has been <b>rejected</b>.</p>
                                            <p><b>Reason:</b> %s</p>
                                            <p>Please contact our support team for assistance.</p>
                                        </div>
                                    </div>
                                    """, merchantName, reasonText));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send review email: {}", e.getMessage());
        }

        return result;
    }

    @Transactional
    public Map<String, Object> verifyCpv(String merchantId, String performedBy) {
        MerchantProfileEntity profile = merchantProfileRepository.findByUserId(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", merchantId));
        if (!"cpv_pending".equals(profile.getOnboardingStatus()))
            throw new BadRequestException("Cannot verify CPV from status: " + profile.getOnboardingStatus());
        profile.setCpvStatus("cpv_verified");
        profile.setCpvVerifiedAt(LocalDateTime.now());
        profile.setCpvVerifiedBy(performedBy);
        profile.setOnboardingStatus("pending_bank_approval");
        merchantProfileRepository.save(profile);
        supportKycActionRepository.save(SupportKycActionEntity.builder()
                .supportStaffId(performedBy).merchantId(profile.getId())
                .action("cpv_verify").decision("cpv_verified").notes("CPV verified").build());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "CPV verified. Application sent to bank for approval.");

        try {
            if (profile.getEmail() != null && !profile.getEmail().isBlank()) {
                String merchantName = profile.getFullName() != null ? profile.getFullName() : "Merchant";
                notificationService.sendEmail(profile.getEmail(),
                        "SabbPe - Shop Verification Complete",
                        String.format("""
                                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                                    <div style="background: #059669; padding: 24px; text-align: center;">
                                        <h1 style="color: white; margin: 0;">Shop Verification Complete</h1>
                                    </div>
                                    <div style="padding: 24px; background: #f9fafb;">
                                        <p>Dear %s,</p>
                                        <p>Your shop verification (CPV) has been completed successfully.</p>
                                        <p>Your application has been sent to the bank for final approval. You will receive an email once the bank completes its review.</p>
                                    </div>
                                </div>
                                """, merchantName));
            }
        } catch (Exception e) {
            log.warn("Failed to send CPV verified email: {}", e.getMessage());
        }

        return result;
    }

    @Transactional
    public Map<String, Object> rejectCpv(String merchantId, String reason) {
        MerchantProfileEntity profile = merchantProfileRepository.findByUserId(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "userId", merchantId));
        if (!"cpv_pending".equals(profile.getOnboardingStatus()))
            throw new BadRequestException("Cannot reject CPV from status: " + profile.getOnboardingStatus());
        profile.setCpvStatus("cpv_rejected");
        profile.setCpvVideoPath(null);
        profile.setCpvRejectionReason(reason);
        merchantProfileRepository.save(profile);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "CPV rejected.");
        return result;
    }

    public List<Map<String, Object>> getKycList() {
        return merchantProfileRepository.findAll().stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("user_id", m.getUserId());
            map.put("full_name", m.getFullName());
            map.put("email", m.getEmail());
            map.put("onboarding_status", m.getOnboardingStatus());
            map.put("pan_number", m.getPanNumber());
            map.put("aadhaar_number", m.getAadhaarNumber());
            map.put("business_name", m.getBusinessName());
            map.put("gst_number", m.getGstNumber());
            map.put("entity_type", m.getEntityType());
            map.put("mobile_number", m.getMobileNumber());
            map.put("score", m.getOnboardingScore() != null ? m.getOnboardingScore() : 0);
            map.put("created_at", m.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAssignedKycForSupport(String supportId) {
        List<TicketEntity> tickets = ticketRepository.findByModuleAndAssignedTo("merchant_onboarding", supportId);
        if (tickets.isEmpty()) return List.of();
        Set<String> userIds = tickets.stream().map(TicketEntity::getCreatedBy)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MerchantProfileEntity> merchants = new ArrayList<>();
        for (String uid : userIds) merchantProfileRepository.findByUserId(uid).ifPresent(merchants::add);
        return merchants.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("user_id", m.getUserId());
            map.put("full_name", m.getFullName());
            map.put("email", m.getEmail());
            map.put("onboarding_status", m.getOnboardingStatus());
            map.put("pan_number", m.getPanNumber());
            map.put("business_name", m.getBusinessName());
            map.put("gst_number", m.getGstNumber());
            map.put("mobile_number", m.getMobileNumber());
            map.put("score", m.getOnboardingScore() != null ? m.getOnboardingScore() : 0);
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllDocuments(String status, String documentType) {
        List<MerchantDocumentEntity> docs = documentRepository.findAll();
        if (status != null) docs = docs.stream().filter(d -> status.equals(d.getStatus())).collect(Collectors.toList());
        if (documentType != null) docs = docs.stream().filter(d -> documentType.equals(d.getDocumentType())).collect(Collectors.toList());
        Set<String> mIds = docs.stream().map(MerchantDocumentEntity::getMerchantId).collect(Collectors.toSet());
        Map<String, String> nameMap = new HashMap<>();
        for (String mid : mIds) merchantProfileRepository.findById(mid)
                .ifPresent(m -> nameMap.put(mid, m.getBusinessName() != null ? m.getBusinessName() : m.getFullName()));
        return docs.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("merchant_id", d.getMerchantId());
            map.put("merchant_name", nameMap.getOrDefault(d.getMerchantId(), "Unknown"));
            map.put("document_type", d.getDocumentType());
            map.put("file_name", d.getFileName());
            map.put("file_path", d.getFilePath());
            map.put("status", d.getStatus());
            map.put("rejection_reason", d.getRejectionReason());
            map.put("uploaded_at", d.getUploadedAt());
            map.put("verified_at", d.getVerifiedAt());
            if (d.getFilePath() != null && !d.getFilePath().isBlank()) {
                map.put("public_url", buildPublicUrl(d.getFilePath()));
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> reviewDocument(String documentId, String status, String rejectionReason) {
        if (!"verified".equals(status) && !"rejected".equals(status))
            throw new BadRequestException("Invalid request");
        if ("rejected".equals(status) && (rejectionReason == null || rejectionReason.isBlank()))
            throw new BadRequestException("rejection_reason required when rejecting");
        MerchantDocumentEntity doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        doc.setStatus(status);
        doc.setVerifiedAt(LocalDateTime.now());
        if ("rejected".equals(status)) doc.setRejectionReason(rejectionReason);
        documentRepository.save(doc);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("document", toMap(doc));
        return result;
    }

    private Map<String, Object> toMap(TicketEntity t) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", t.getId());
        map.put("module", t.getModule());
        map.put("reference_id", t.getReferenceId());
        map.put("title", t.getTitle());
        map.put("description", t.getDescription());
        map.put("priority", t.getPriority());
        map.put("status", t.getStatus());
        map.put("created_by", t.getCreatedBy());
        map.put("assigned_to", t.getAssignedTo());
        map.put("created_at", t.getCreatedAt());
        map.put("updated_at", t.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toMapWithDetails(TicketEntity t) {
        Map<String, Object> map = toMap(t);
        merchantProfileRepository.findByUserId(t.getCreatedBy()).ifPresent(m -> {
            map.put("merchant_name", m.getFullName());
            map.put("merchant_email", m.getEmail());
            map.put("merchant_phone", m.getMobileNumber());
        });
        List<TicketMessageEntity> messages = ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(t.getId());
        if (!messages.isEmpty()) {
            TicketMessageEntity last = messages.get(messages.size() - 1);
            map.put("last_message_role", last.getSenderRole());
            map.put("last_message_at", last.getCreatedAt());
        }
        return map;
    }

    private Map<String, Object> toMap(MerchantProfileEntity m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("user_id", m.getUserId());
        map.put("full_name", m.getFullName());
        map.put("mobile_number", m.getMobileNumber());
        map.put("email", m.getEmail());
        map.put("pan_number", m.getPanNumber());
        map.put("aadhaar_number", m.getAadhaarNumber());
        map.put("business_name", m.getBusinessName());
        map.put("gst_number", m.getGstNumber());
        map.put("entity_type", m.getEntityType());
        map.put("onboarding_status", m.getOnboardingStatus());
        map.put("onboarding_score", m.getOnboardingScore());
        map.put("rejection_reason", m.getRejectionReason());
        map.put("application_id", m.getApplicationId());
        map.put("cpv_status", m.getCpvStatus());
        map.put("cpv_video_path", m.getCpvVideoPath());
        map.put("cpv_submitted", m.getCpvSubmitted());
        map.put("cpv_submitted_at", m.getCpvSubmittedAt());
        map.put("cpv_verified_at", m.getCpvVerifiedAt());
        map.put("cpv_verified_by", m.getCpvVerifiedBy());
        map.put("cpv_rejection_reason", m.getCpvRejectionReason());
        map.put("risk_level", m.getRiskLevel());
        map.put("selected_products", m.getSelectedProducts());
        map.put("total_monthly_cost", m.getTotalMonthlyCost());
        map.put("total_onetime_cost", m.getTotalOnetimeCost());
        map.put("total_integration_cost", m.getTotalIntegrationCost());
        map.put("submitted_at", m.getSubmittedAt());
        map.put("cancelled_cheque_url", m.getCancelledChequeUrl());
        map.put("pan_card_url", m.getPanCardUrl());
        map.put("aadhaar_card_url", m.getAadhaarCardUrl());
        map.put("business_proof_url", m.getBusinessProofUrl());
        map.put("registration_details", m.getRegistrationDetails());
        map.put("split_payment_config", m.getSplitPaymentConfig());
        map.put("created_at", m.getCreatedAt());
        map.put("updated_at", m.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toMap(MerchantDocumentEntity d) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", d.getId());
        map.put("merchant_id", d.getMerchantId());
        map.put("document_type", d.getDocumentType());
        map.put("file_name", d.getFileName());
        map.put("file_path", d.getFilePath());
        map.put("status", d.getStatus());
        map.put("rejection_reason", d.getRejectionReason());
        map.put("uploaded_at", d.getUploadedAt());
        map.put("verified_at", d.getVerifiedAt());
        return map;
    }

    private Map<String, Object> toMessageMap(TicketMessageEntity m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("ticket_id", m.getTicketId());
        map.put("sender_id", m.getSenderId());
        map.put("sender_role", m.getSenderRole());
        map.put("message", m.getMessage());
        map.put("created_at", m.getCreatedAt());
        return map;
    }
}
