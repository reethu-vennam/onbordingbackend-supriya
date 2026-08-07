package com.sabbpe.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.dto.EcosystemOrganizationRequest;
import com.sabbpe.model.*;
import com.sabbpe.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcosystemSyncService {

    @Value("${app.ecosystem.base-url:}")
    private String baseUrl;

    @Value("${app.ecosystem.api-key:}")
    private String apiKey;

    @Value("${app.ecosystem.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantKycRepository merchantKycRepository;
    private final MerchantBankDetailRepository merchantBankDetailRepository;
    private final MerchantPersonRepository merchantPersonRepository;
    private final MerchantDocumentRepository merchantDocumentRepository;
    private final EcosystemSyncLogRepository syncLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_RETRY_ATTEMPTS = 3;

    public void sync(String merchantId) {
        if (!enabled) {
            log.info("Ecosystem sync is disabled, skipping for merchant {}", merchantId);
            return;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            log.info("Ecosystem base URL not configured, skipping sync for merchant {}", merchantId);
            return;
        }

        MerchantProfileEntity merchant = merchantProfileRepository.findById(merchantId).orElse(null);
        if (merchant == null || !"approved".equals(merchant.getOnboardingStatus())) {
            log.debug("Merchant {} not found or not approved, skipping sync", merchantId);
            return;
        }

        EcosystemOrganizationRequest payload = buildPayload(merchant);
        doSync(merchantId, payload, 1);
    }

    public void retry(String merchantId) {
        if (!enabled) return;
        if (baseUrl == null || baseUrl.isBlank()) return;

        MerchantProfileEntity merchant = merchantProfileRepository.findById(merchantId).orElse(null);
        if (merchant == null || !"approved".equals(merchant.getOnboardingStatus())) {
            log.debug("Merchant {} not found or not approved, skipping retry", merchantId);
            return;
        }

        List<EcosystemSyncLogEntity> existingLogs = syncLogRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        int nextAttempt = existingLogs.isEmpty() ? 1 : existingLogs.stream()
                .mapToInt(EcosystemSyncLogEntity::getAttemptCount)
                .max().orElse(0) + 1;

        EcosystemOrganizationRequest payload = buildPayload(merchant);
        doSync(merchantId, payload, nextAttempt);
    }

    private void doSync(String merchantId, EcosystemOrganizationRequest payload, int attemptCount) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(payloadJson, headers);
            String url = baseUrl;

            log.info("Syncing merchant {} to ecosystem API (attempt {})", merchantId, attemptCount);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                saveSyncLog(merchantId, "SUCCESS", attemptCount, null, payloadJson);
                log.info("Successfully synced merchant {} to ecosystem", merchantId);
            } else {
                String error = "HTTP " + response.getStatusCodeValue() + ": " + response.getBody();
                saveSyncLog(merchantId, "FAILED", attemptCount, error, payloadJson);
                log.error("Ecosystem sync failed for merchant {}: {}", merchantId, error);
            }
        } catch (Exception e) {
            String error = e.getClass().getSimpleName() + ": " + e.getMessage();
            saveSyncLog(merchantId, "FAILED", attemptCount, error,
                    safelySerialize(payload));
            log.error("Ecosystem sync failed for merchant {}: {}", merchantId, error);
        }
    }

    private void saveSyncLog(String merchantId, String status, int attemptCount,
                              String errorMessage, String payloadJson) {
        EcosystemSyncLogEntity logEntry = new EcosystemSyncLogEntity();
        logEntry.setMerchantId(merchantId);
        logEntry.setStatus(status);
        logEntry.setAttemptCount(attemptCount);
        logEntry.setLastAttempt(LocalDateTime.now());
        logEntry.setErrorMessage(errorMessage);
        logEntry.setPayload(payloadJson);
        syncLogRepository.save(logEntry);
    }

    EcosystemOrganizationRequest buildPayload(MerchantProfileEntity m) {
        String kycStatus = "PENDING";
        Optional<MerchantKycEntity> kycOpt = merchantKycRepository.findByMerchantId(m.getId());
        if (kycOpt.isPresent() && kycOpt.get().getKycStatus() != null) {
            kycStatus = kycOpt.get().getKycStatus();
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("onboardedBy", "SABBPE");
        metadata.put("entityType", m.getEntityType());
        metadata.put("kycStatus", kycStatus != null ? kycStatus.toUpperCase() : "PENDING");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return EcosystemOrganizationRequest.builder()
                .organizationId(m.getId())
                .parentOrganizationId(m.getDistributorId())
                .organizationCode(buildOrgCode(m.getBusinessName()))
                .legalName(m.getFullName())
                .tradeName(m.getBusinessName())
                .email(m.getEmail())
                .mobile(m.getMobileNumber())
                .gstNumber(m.getGstNumber())
                .panNumber(m.getPanNumber())
                .countryCode("IN")
                .currencyCode("INR")
                .timezoneName("Asia/Kolkata")
                .status("1")
                .metadata(metadata)
                .versionNo("1")
                .organizationType(m.getEntityType())
                .bankAccounts(buildBankAccounts(m.getId()))
                .contacts(buildContacts(m.getId()))
                .documents(buildDocuments(m.getId()))
                .products(parseJsonArray(m.getSelectedProducts()))
                .createdOn(m.getCreatedAt() != null ? m.getCreatedAt().format(fmt) : null)
                .modifiedOn(m.getUpdatedAt() != null ? m.getUpdatedAt().format(fmt) : null)
                .secretKey(apiKey)
                .build();
    }

    private String buildOrgCode(String businessName) {
        if (businessName == null || businessName.isBlank()) {
            return null;
        }
        return businessName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    }

    private Object buildBankAccounts(String merchantId) {
        Optional<MerchantBankDetailEntity> optBank = merchantBankDetailRepository.findByMerchantId(merchantId);
        if (optBank.isPresent() && optBank.get().getBankDetailsJson() != null) {
            return parseJsonArray(optBank.get().getBankDetailsJson());
        }
        return null;
    }

    private Object buildContacts(String merchantId) {
        List<MerchantPersonEntity> persons = merchantPersonRepository.findByMerchantIdOrderBySequenceOrderAsc(merchantId);
        if (persons.isEmpty()) return null;

        List<Map<String, Object>> contacts = new ArrayList<>();
        for (MerchantPersonEntity p : persons) {
            Map<String, Object> contact = new LinkedHashMap<>();
            contact.put("full_name", p.getFullName());
            contact.put("role", p.getRole());
            contact.put("pan_number", p.getPanNumber());
            contact.put("is_authorized_signatory", p.getIsAuthorizedSignatory());
            contacts.add(contact);
        }
        return contacts;
    }

    private Object buildDocuments(String merchantId) {
        List<MerchantDocumentEntity> docs = merchantDocumentRepository.findByMerchantId(merchantId);
        if (docs.isEmpty()) return null;

        List<Map<String, Object>> result = new ArrayList<>();
        for (MerchantDocumentEntity d : docs) {
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("document_type", d.getDocumentType());
            doc.put("file_name", d.getFileName());
            doc.put("file_path", d.getFilePath());
            doc.put("status", d.getStatus());
            doc.put("mime_type", d.getMimeType());
            doc.put("document_category", d.getDocCategory());
            result.add(doc);
        }
        return result;
    }

    private Object parseJsonArray(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Object>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON: {}", e.getMessage());
            return null;
        }
    }

    private String safelySerialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
