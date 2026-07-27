package com.sabbpe.service;

import com.sabbpe.dto.*;
import com.sabbpe.exception.BadRequestException;
import com.sabbpe.exception.ResourceNotFoundException;
import com.sabbpe.model.DistributorProfileEntity;
import com.sabbpe.repository.DistributorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributorOnboardingService {

    private final DistributorProfileRepository distributorProfileRepository;
    private final NotificationService notificationService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private static final List<String> VALID_AGREEMENT_STATUSES = List.of(
            "pending", "sent", "uploaded", "approved", "rejected", "credentials_sent", "onboarding_completed"
    );

    // ── GET /me ──────────────────────────────────────────────────────────────

    public DistributorOnboardingStatusResponse getStatus(String userId) {
        DistributorProfileEntity profile = distributorProfileRepository.findByUserId(userId).orElse(null);

        if (profile == null) {
            profile = new DistributorProfileEntity();
            profile.setId(UUID.randomUUID().toString());
            profile.setUserId(userId);
            profile.setCompanyName("Distributor");
            profile.setContactPerson("Distributor");
            profile.setEmail("");
            profile.setMobileNumber("");
            profile.setIsActive(true);
            distributorProfileRepository.save(profile);
            log.info("Auto-created distributor profile for user {}", userId);

            return DistributorOnboardingStatusResponse.builder()
                    .id(profile.getId())
                    .companyName(profile.getCompanyName())
                    .agreementStatus("pending")
                    .kycStatus("pending")
                    .canSubmitKyc(true)
                    .canSignAgreement(true)
                    .build();
        }

        return DistributorOnboardingStatusResponse.builder()
                .id(profile.getId())
                .companyName(profile.getCompanyName())
                .contactPerson(profile.getContactPerson())
                .email(profile.getEmail())
                .mobileNumber(profile.getMobileNumber())
                .agreementStatus(profile.getAgreementStatus())
                .kycStatus(profile.getKycStatus())
                .panVerified(profile.getPanVerified() != null && profile.getPanVerified())
                .aadhaarVerified(profile.getAadhaarVerified() != null && profile.getAadhaarVerified())
                .bankVerified(profile.getBankVerified() != null && profile.getBankVerified())
                .kycSubmittedAt(profile.getKycSubmittedAt())
                .agreementSentAt(profile.getAgreementSentAt())
                .onboardingCompletedAt(profile.getOnboardingCompletedAt())
                .canSubmitKyc("pending".equals(profile.getKycStatus()) || "rejected".equals(profile.getKycStatus()))
                .canSignAgreement("pending".equals(profile.getAgreementStatus()) || "rejected".equals(profile.getAgreementStatus()))
                .build();
    }

    // ── POST /agreement/send (admin) ─────────────────────────────────────────

    @Transactional
    public void sendAgreement(String adminUserId, SendAgreementRequest request) {
        if (request.getDistributorId() == null) {
            throw new BadRequestException("distributorId is required");
        }
        if (request.getAgreementFilePath() == null && request.getFileBase64() == null) {
            throw new BadRequestException("Either agreementFilePath or fileBase64+fileName is required");
        }

        DistributorProfileEntity profile = distributorProfileRepository.findById(request.getDistributorId())
                .orElseThrow(() -> new ResourceNotFoundException("Distributor", "id", request.getDistributorId()));

        if (!"pending".equals(profile.getAgreementStatus()) && !"rejected".equals(profile.getAgreementStatus())) {
            throw new BadRequestException("Agreement status must be 'pending' or 'rejected' to send");
        }

        String agreementFilePath = request.getAgreementFilePath();

        // If fileBase64 provided, save to disk
        if (request.getFileBase64() != null && request.getFileName() != null) {
            try {
                byte[] fileBytes = Base64.getDecoder().decode(request.getFileBase64());
                String ext = "";
                if (request.getFileName().contains(".")) {
                    ext = request.getFileName().substring(request.getFileName().lastIndexOf("."));
                }
                String filename = "agreement-" + System.currentTimeMillis() + ext;
                Path uploadPath = Paths.get(uploadDir, "agreements", request.getDistributorId());
                Files.createDirectories(uploadPath);
                Path filePath = uploadPath.resolve(filename);
                Files.write(filePath, fileBytes);
                agreementFilePath = "/uploads/agreements/" + request.getDistributorId() + "/" + filename;
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid base64 file data");
            } catch (IOException e) {
                throw new BadRequestException("Failed to save agreement file");
            }
        }

        String token = UUID.randomUUID().toString();
        profile.setAgreementStatus("sent");
        profile.setAgreementFilePath(agreementFilePath);
        profile.setAgreementSentAt(LocalDateTime.now());
        profile.setAgreementSentBy(adminUserId);
        profile.setOnboardingToken(token);
        profile.setOnboardingTokenExpiresAt(LocalDateTime.now().plusDays(14));
        profile.setOnboardingTokenUsedAt(null);
        distributorProfileRepository.save(profile);

        // Send email
        String onboardingUrl = frontendUrl + "/distributor-onboarding?token=" + token;
        try {
            notificationService.sendDistributorAgreementEmail(
                    profile.getEmail(), profile.getCompanyName(), onboardingUrl
            );
        } catch (Exception e) {
            log.warn("Failed to send agreement email: {}", e.getMessage());
        }
    }

    // ── GET /agreement/download?token= (public) ─────────────────────────────

    public AgreementDownloadResponse getAgreementByToken(String token) {
        DistributorProfileEntity profile = distributorProfileRepository.findByOnboardingToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid token"));

        if (profile.getOnboardingTokenUsedAt() != null) {
            throw new BadRequestException("Token already used");
        }
        if (profile.getOnboardingTokenExpiresAt() != null
                && profile.getOnboardingTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token expired");
        }
        if (!"sent".equals(profile.getAgreementStatus()) && !"rejected".equals(profile.getAgreementStatus())) {
            throw new BadRequestException("Agreement not available for download");
        }

        return AgreementDownloadResponse.builder()
                .companyName(profile.getCompanyName())
                .agreementStatus(profile.getAgreementStatus())
                .agreementRejectionReason(profile.getAgreementRejectionReason())
                .agreementDownloadUrl(profile.getAgreementFilePath())
                .build();
    }

    // ── POST /agreement/upload-signed?token= (public) ───────────────────────

    @Transactional
    public void uploadSignedAgreement(String token, UploadSignedAgreementRequest request) {
        DistributorProfileEntity profile = distributorProfileRepository.findByOnboardingToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid token"));

        if (profile.getOnboardingTokenUsedAt() != null) {
            throw new BadRequestException("Token already used");
        }
        if (profile.getOnboardingTokenExpiresAt() != null
                && profile.getOnboardingTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token expired");
        }
        if (!"sent".equals(profile.getAgreementStatus()) && !"rejected".equals(profile.getAgreementStatus())) {
            throw new BadRequestException("Agreement not in a state that can be uploaded");
        }

        if (request.getFileBase64() == null) {
            throw new BadRequestException("fileBase64 is required");
        }

        try {
            byte[] fileBytes = Base64.getDecoder().decode(request.getFileBase64());
            String ext = "";
            if (request.getFileName() != null && request.getFileName().contains(".")) {
                ext = request.getFileName().substring(request.getFileName().lastIndexOf("."));
            }
            String filename = "signed-" + System.currentTimeMillis() + ext;
            Path uploadPath = Paths.get(uploadDir, "agreements", profile.getUserId());
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, fileBytes);

            profile.setSignedAgreementPath("/uploads/agreements/" + profile.getUserId() + "/" + filename);
            profile.setAgreementUploadedAt(LocalDateTime.now());
            profile.setAgreementStatus("uploaded");
            profile.setAgreementRejectionReason(null);
            profile.setOnboardingTokenUsedAt(LocalDateTime.now());
            distributorProfileRepository.save(profile);

            log.info("Signed agreement uploaded for distributor {}", profile.getUserId());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid base64 file data");
        } catch (IOException e) {
            throw new BadRequestException("Failed to save signed agreement file");
        }
    }

    // ── GET /signed/:distributorId ──────────────────────────────────────────

    public Map<String, Object> getSignedAgreementUrl(String callerUserId, String distributorId, boolean isAdmin) {
        DistributorProfileEntity profile;
        if (isAdmin) {
            profile = distributorProfileRepository.findById(distributorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Distributor", "id", distributorId));
        } else {
            profile = distributorProfileRepository.findByUserId(callerUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Distributor", "userId", callerUserId));
            if (!profile.getUserId().equals(callerUserId)) {
                throw new BadRequestException("Access denied");
            }
        }

        if (profile.getSignedAgreementPath() == null) {
            throw new BadRequestException("No signed agreement found");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("signedUrl", profile.getSignedAgreementPath());
        result.put("companyName", profile.getCompanyName());
        return result;
    }

    // ── POST /approve (admin) ───────────────────────────────────────────────

    @Transactional
    public void approveAgreement(String distributorId, String adminUserId) {
        if (distributorId == null) {
            throw new BadRequestException("distributorId is required");
        }
        DistributorProfileEntity profile = distributorProfileRepository.findById(distributorId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor", "id", distributorId));

        if (!"uploaded".equals(profile.getAgreementStatus())) {
            throw new BadRequestException("Agreement status must be 'uploaded' to approve");
        }

        profile.setAgreementStatus("approved");
        profile.setAgreementApprovedAt(LocalDateTime.now());
        profile.setAgreementApprovedBy(adminUserId);
        distributorProfileRepository.save(profile);
    }

    // ── POST /reject (admin) ────────────────────────────────────────────────

    @Transactional
    public void rejectAgreement(String distributorId, String reason) {
        if (distributorId == null) {
            throw new BadRequestException("distributorId is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Rejection reason is required");
        }

        DistributorProfileEntity profile = distributorProfileRepository.findById(distributorId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor", "id", distributorId));

        if (!"uploaded".equals(profile.getAgreementStatus())) {
            throw new BadRequestException("Agreement status must be 'uploaded' to reject");
        }

        profile.setAgreementStatus("rejected");
        profile.setAgreementRejectionReason(reason.trim());
        distributorProfileRepository.save(profile);
    }

    // ── POST /credentials/send (admin) ──────────────────────────────────────

    @Transactional
    public Map<String, Object> sendCredentials(String distributorId, String password, String adminUserId) {
        if (distributorId == null || password == null) {
            throw new BadRequestException("distributorId and password are required");
        }
        if (password.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }

        DistributorProfileEntity profile = distributorProfileRepository.findById(distributorId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor", "id", distributorId));

        String status = profile.getAgreementStatus();
        if (!"approved".equals(status) && !"credentials_sent".equals(status) && !"onboarding_completed".equals(status)) {
            throw new BadRequestException("Agreement must be approved before sending credentials");
        }

        String newStatus = "approved".equals(status) ? "credentials_sent" : status;
        profile.setAgreementStatus(newStatus);
        profile.setCredentialsSentAt(LocalDateTime.now());
        profile.setCredentialsSentBy(adminUserId);

        if (!"onboarding_completed".equals(status)) {
            profile.setOnboardingCompletedAt(LocalDateTime.now());
        }

        distributorProfileRepository.save(profile);

        try {
            notificationService.sendDistributorCredentialsEmail(
                    profile.getEmail(), profile.getCompanyName(), password
            );
        } catch (Exception e) {
            log.warn("Failed to send credentials email: {}", e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("email", profile.getEmail());
        result.put("distributorName", profile.getCompanyName());
        result.put("userId", profile.getUserId());
        return result;
    }

    // ── POST /kyc/submit ────────────────────────────────────────────────────

    @Transactional
    public List<String> submitKyc(String userId, DistributorKycRequest request) {
        DistributorProfileEntity profile = getProfileByUserId(userId);

        if (!"pending".equals(profile.getKycStatus()) && !"rejected".equals(profile.getKycStatus())) {
            throw new BadRequestException("KYC already submitted or approved");
        }

        List<String> missingFields = new ArrayList<>();
        if (request.getPanNumber() == null || request.getPanNumber().isBlank()) missingFields.add("pan_number");
        if (request.getAadhaarLast4() == null || request.getAadhaarLast4().isBlank()) missingFields.add("aadhaar_last4");
        if (request.getBankAccountNumber() == null || request.getBankAccountNumber().isBlank()) missingFields.add("bank_account_number");
        if (request.getBankIfsc() == null || request.getBankIfsc().isBlank()) missingFields.add("bank_ifsc");
        if (request.getAddress() == null || request.getAddress().isBlank()) missingFields.add("address");

        if (!missingFields.isEmpty()) {
            throw new BadRequestException("Missing required fields: " + String.join(", ", missingFields));
        }

        profile.setPanNumber(request.getPanNumber());
        profile.setAadhaarLast4(request.getAadhaarLast4());
        profile.setPanDocumentPath(request.getPanDocumentPath());
        profile.setAadhaarDocumentPath(request.getAadhaarDocumentPath());
        profile.setProfilePhotoPath(request.getProfilePhotoPath());
        profile.setBankAccountNumber(request.getBankAccountNumber());
        profile.setBankIfsc(request.getBankIfsc());
        profile.setAddress(request.getAddress());
        profile.setKycStatus("submitted");
        profile.setKycSubmittedAt(LocalDateTime.now());
        distributorProfileRepository.save(profile);

        log.info("Distributor {} submitted KYC", userId);
        return missingFields;
    }

    // ── POST /kyc/approve (admin) ───────────────────────────────────────────

    @Transactional
    public void approveKyc(String distributorId, String adminUserId) {
        DistributorProfileEntity profile = distributorProfileRepository.findById(distributorId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor", "id", distributorId));

        if (!"submitted".equals(profile.getKycStatus())) {
            throw new BadRequestException("KYC not in submitted state");
        }

        profile.setPanVerified(true);
        profile.setAadhaarVerified(true);
        profile.setKycStatus("approved");
        profile.setKycVerifiedAt(LocalDateTime.now());
        profile.setKycVerifiedBy(adminUserId);
        profile.setKycUpdatedAt(LocalDateTime.now());
        distributorProfileRepository.save(profile);

        notificationService.sendEmail(profile.getEmail(),
                "KYC Approved",
                "<h2>Your KYC has been approved</h2>");
    }

    // ── POST /kyc/reject (admin) ────────────────────────────────────────────

    @Transactional
    public void rejectKyc(String distributorId, String reason, String adminUserId) {
        DistributorProfileEntity profile = distributorProfileRepository.findById(distributorId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor", "id", distributorId));

        if (!"submitted".equals(profile.getKycStatus())) {
            throw new BadRequestException("KYC not in submitted state");
        }

        profile.setKycStatus("rejected");
        profile.setKycVerifiedBy(adminUserId);
        profile.setKycUpdatedAt(LocalDateTime.now());
        distributorProfileRepository.save(profile);
    }

    // ── GET / (list distributors, admin only) ───────────────────────────────

    public List<Map<String, Object>> listDistributors() {
        return distributorProfileRepository.findAll().stream()
                .sorted((a, b) -> {
                    LocalDateTime aCreated = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime bCreated = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                    return bCreated.compareTo(aCreated);
                })
                .map(profile -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", profile.getId());
                    row.put("user_id", profile.getUserId());
                    row.put("company_name", profile.getCompanyName());
                    row.put("contact_person", profile.getContactPerson());
                    row.put("email", profile.getEmail());
                    row.put("mobile_number", profile.getMobileNumber());
                    row.put("agreement_status", profile.getAgreementStatus());
                    row.put("kyc_status", profile.getKycStatus());
                    row.put("agreement_sent_at", profile.getAgreementSentAt());
                    row.put("agreement_uploaded_at", profile.getAgreementUploadedAt());
                    row.put("created_at", profile.getCreatedAt());
                    return row;
                })
                .toList();
    }

    private DistributorProfileEntity getProfileByUserId(String userId) {
        return distributorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Distributor", "userId", userId));
    }
}
