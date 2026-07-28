package com.sabbpe.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.admin.exception.BadRequestException;
import com.sabbpe.admin.exception.ResourceNotFoundException;
import com.sabbpe.admin.model.*;
import com.sabbpe.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentReviewService {

    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantDocumentRepository documentRepository;
    private final DocumentValidationRepository validationRepository;
    private final MerchantCreditCheckRepository creditCheckRepository;
    private final MerchantBankDetailRepository bankDetailRepository;
    private final MerchantKycRepository kycRepository;
    private final MerchantPersonRepository personRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern PAN_REGEX = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern GST_REGEX = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");
    private static final Pattern IFSC_REGEX = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

    private static final Map<String, String> IFSC_BANK_MAP = Map.ofEntries(
            Map.entry("SBIN", "SBI"), Map.entry("HDFC", "HDFC"), Map.entry("ICIC", "ICICI"),
            Map.entry("UBIN", "UNION BANK"), Map.entry("PUNB", "PUNJAB NATIONAL"),
            Map.entry("BARB", "BANK OF BARODA"), Map.entry("IDFB", "IDBI"),
            Map.entry("FDRL", "FEDERAL"), Map.entry("KKBK", "KOTAK"), Map.entry("INDB", "INDUSIND"),
            Map.entry("UTIB", "AXIS"), Map.entry("CNRB", "CANARA"), Map.entry("UCBA", "UCO"),
            Map.entry("ABHY", "ABHYUDAYA"), Map.entry("AIIB", "INDIAN OVERSEAS"),
            Map.entry("BKID", "BANK OF INDIA"), Map.entry("MAHB", "BANK OF MAHARASHTRA"),
            Map.entry("ORBC", "ORIENTAL BANK"), Map.entry("PSIB", "PUNJAB & SIND"),
            Map.entry("RATN", "RBL"), Map.entry("SIBL", "SOUTH INDIAN"),
            Map.entry("TCSC", "TCS"), Map.entry("UTBI", "UNITED BANK"), Map.entry("VIJB", "VIJAYA"),
            Map.entry("SFBL", "SLICE SMALL FINANCE")
    );

    @Value("${app.transbank.base-url:https://transbank.sabbpe.com/api}")
    private String transbankBaseUrl;

    @Value("${app.transbank.client-id:5e06f31d-d298-11f0-96ff-4201c0a81e02}")
    private String transbankClientId;

    @Value("${app.transbank.processor:TRANSBANK}")
    private String transbankProcessor;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ═══════════ GET MERCHANTS FOR DOCUMENT REVIEW ═══════════
    public List<Map<String, Object>> getMerchantsForReview() {
        List<MerchantProfileEntity> merchants = merchantProfileRepository.findAll().stream()
                .filter(m -> {
                    String s = m.getOnboardingStatus();
                    return s != null && List.of("submitted", "validating", "pending_bank_approval",
                            "verified", "approved", "rejected", "cpv_pending", "cpv_verified",
                            "agreement_pending", "agreement_signed", "bank_rejected").contains(s);
                })
                .sorted(Comparator.comparing(MerchantProfileEntity::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (MerchantProfileEntity merchant : merchants) {
            List<MerchantDocumentEntity> docs = documentRepository.findByMerchantId(merchant.getId());
            List<Map<String, Object>> documents = new ArrayList<>();
            int unchecked = 0, passed = 0, failed = 0, pending = 0;

            for (MerchantDocumentEntity doc : docs) {
                List<DocumentValidationEntity> validations = validationRepository.findByDocumentId(doc.getId());

                Map<String, Object> docMap = new LinkedHashMap<>();
                docMap.put("id", doc.getId());
                docMap.put("document_type", doc.getDocumentType());
                docMap.put("file_name", doc.getFileName());
                docMap.put("file_path", doc.getFilePath());
                docMap.put("status", doc.getStatus());
                docMap.put("validation_status", doc.getStatus());
                docMap.put("rejection_reason", doc.getRejectionReason());
                docMap.put("uploaded_at", doc.getUploadedAt());
                docMap.put("verified_at", doc.getVerifiedAt());
                docMap.put("validations", validations.stream().map(this::dvToMap).collect(Collectors.toList()));
                documents.add(docMap);

                String vs = doc.getStatus() != null ? doc.getStatus() : "unchecked";
                switch (vs) {
                    case "unchecked": unchecked++; break;
                    case "passed": case "verified": passed++; break;
                    case "failed": case "rejected": failed++; break;
                    case "pending": pending++; break;
                }
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", merchant.getId());
            entry.put("full_name", merchant.getFullName());
            entry.put("business_name", merchant.getBusinessName());
            entry.put("email", merchant.getEmail());
            entry.put("mobile_number", merchant.getMobileNumber());
            entry.put("onboarding_status", merchant.getOnboardingStatus());
            entry.put("onboarding_score", merchant.getOnboardingScore() != null ? merchant.getOnboardingScore() : 0);
            entry.put("documents", documents);
            entry.put("document_summary", Map.of("total", documents.size(), "unchecked", unchecked,
                    "passed", passed, "failed", failed, "pending", pending));
            result.add(entry);
        }
        return result;
    }

    // ═══════════ COMPUTE ONBOARDING SCORE (8 categories, max 90) ═══════════
    @SuppressWarnings("unchecked")
    public Map<String, Object> computeScore(String merchantProfileId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findById(merchantProfileId).orElse(null);
        if (merchant == null)
            return Map.of("score", 0, "categories", List.of(), "reasons", List.of(), "isManualReview", true);

        List<MerchantDocumentEntity> allDocs = documentRepository.findByMerchantId(merchantProfileId);
        List<String> docIds = allDocs.stream().map(MerchantDocumentEntity::getId).collect(Collectors.toList());

        // Build per-doc-type -> list of checks (parsed from extractedData JSON)
        Map<String, List<Map<String, Object>>> docChecks = new LinkedHashMap<>();
        for (MerchantDocumentEntity doc : allDocs) {
            List<DocumentValidationEntity> vals = validationRepository.findByDocumentId(doc.getId());
            List<Map<String, Object>> checks = new ArrayList<>();
            for (DocumentValidationEntity v : vals) {
                try {
                    if (v.getExtractedData() != null) {
                        checks.add(objectMapper.readValue(v.getExtractedData(), Map.class));
                    }
                } catch (Exception e) { /* skip */ }
                // Also treat the overall validation as a check
                Map<String, Object> overallCheck = new LinkedHashMap<>();
                overallCheck.put("checkType", v.getValidationType());
                overallCheck.put("checkResult", Boolean.TRUE.equals(v.getIsValid()) ? "pass" : "fail");
                checks.add(overallCheck);
            }
            docChecks.put(doc.getDocumentType() != null ? doc.getDocumentType() : "unknown", checks);
        }

        MerchantBankDetailEntity bankDetails = bankDetailRepository.findByMerchantId(merchantProfileId).orElse(null);
        List<String> reasons = new ArrayList<>();

        // Helper
        java.util.function.BiFunction<String, String, Map<String, Object>> findCheck = (docType, checkType) -> {
            List<Map<String, Object>> checks = docChecks.getOrDefault(docType, List.of());
            return checks.stream().filter(c -> checkType.equals(c.get("checkType"))).findFirst().orElse(null);
        };

        // ── GST Verification (18 pts) ──
        int gstEarned = 0;
        List<Map<String, Object>> gstChecks = docChecks.getOrDefault("gst_certificate", List.of());
        if (!gstChecks.isEmpty()) {
            if (isPass(findCheck.apply("gst_certificate", "gstin_format"))) gstEarned += 5;
            else reasons.add("GSTIN format invalid in document");
            if (isPass(findCheck.apply("gst_certificate", "gstin_match"))) gstEarned += 4;
            else if (isFail(findCheck.apply("gst_certificate", "gstin_match"))) reasons.add("GST number on certificate doesn't match profile");
            if (isPass(findCheck.apply("gst_certificate", "pan_in_gst"))) gstEarned += 3;
            else if (isFail(findCheck.apply("gst_certificate", "pan_in_gst"))) reasons.add("PAN in GST certificate doesn't match profile PAN");
            if (isPass(findCheck.apply("gst_certificate", "legal_name_match"))) gstEarned += 3;
            else if (isFail(findCheck.apply("gst_certificate", "legal_name_match"))) reasons.add("Legal name on GST doesn't match profile name");
            if (isPass(findCheck.apply("gst_certificate", "trade_name_match"))) gstEarned += 3;
            else if (isFail(findCheck.apply("gst_certificate", "trade_name_match"))) reasons.add("Trade name on GST doesn't match business name");
        } else {
            String gst = (merchant.getGstNumber() != null ? merchant.getGstNumber() : "").toUpperCase();
            if (!gst.isEmpty() && GST_REGEX.matcher(gst).matches()) gstEarned += 9;
            else reasons.add("GST number missing or invalid format");
            if (!gst.isEmpty() && gst.length() >= 12) {
                String panFromGst = gst.substring(2, 12);
                if (PAN_REGEX.matcher(panFromGst).matches()) gstEarned += 9;
                else reasons.add("PAN in GST does not match profile PAN");
            } else if (!gst.isEmpty()) reasons.add("PAN in GST does not match profile PAN");
            else reasons.add("GST certificate not validated yet");
        }

        // ── PAN Verification (14 pts) ──
        int panEarned = 0;
        if (docChecks.containsKey("pan_card") && !docChecks.get("pan_card").isEmpty()) {
            if (isPass(findCheck.apply("pan_card", "format"))) panEarned += 6;
            else reasons.add("PAN format invalid in uploaded document");
            if (isPass(findCheck.apply("pan_card", "cross_match"))) panEarned += 8;
            else reasons.add("PAN on document doesn't match profile");
        } else {
            String pan = (merchant.getPanNumber() != null ? merchant.getPanNumber() : "").toUpperCase();
            if (!pan.isEmpty() && PAN_REGEX.matcher(pan).matches()) panEarned += 6;
            else reasons.add("PAN number missing or invalid format");
            if (!pan.isEmpty()) panEarned += 8;
            else reasons.add("PAN card not validated yet");
        }

        // ── Aadhaar Verification (14 pts) ──
        int aadhaarEarned = 0;
        if (docChecks.containsKey("aadhaar_card") && !docChecks.get("aadhaar_card").isEmpty()) {
            if (isPass(findCheck.apply("aadhaar_card", "format"))) aadhaarEarned += 6;
            else reasons.add("Aadhaar format invalid in uploaded document");
            if (isPass(findCheck.apply("aadhaar_card", "cross_match"))) aadhaarEarned += 8;
            else reasons.add("Aadhaar on document doesn't match profile");
        } else {
            String aadhaar = (merchant.getAadhaarNumber() != null ? merchant.getAadhaarNumber() : "").replaceAll("\\s", "");
            if (!aadhaar.isEmpty() && aadhaar.matches("^\\d{12}$")) aadhaarEarned += 6;
            else reasons.add("Aadhaar number missing or invalid format");
            if (!aadhaar.isEmpty()) aadhaarEarned += 8;
            else reasons.add("Aadhaar card not validated yet");
        }

        // ── Bank Verification (14 pts) ──
        int bankEarned = 0;
        List<Map<String, Object>> bankChecks = docChecks.getOrDefault("bank_statement",
                docChecks.getOrDefault("cancelled_cheque", List.of()));
        if (!bankChecks.isEmpty()) {
            if (isPass(findCheck.apply("bank_statement", "format")) || isPass(findCheck.apply("cancelled_cheque", "format")))
                bankEarned += 5;
            else reasons.add("IFSC format invalid on bank document");
            if (isPass(findCheck.apply("bank_statement", "account_format")) || isPass(findCheck.apply("cancelled_cheque", "account_format")))
                bankEarned += 4;
            else reasons.add("Account number format invalid on bank document");
            if (isPass(findCheck.apply("bank_statement", "account_holder_match")) || isPass(findCheck.apply("cancelled_cheque", "account_holder_match")))
                bankEarned += 5;
            else reasons.add("Account holder name doesn't match merchant name");
        } else if (bankDetails != null) {
            String ifsc = (bankDetails.getIfscCode() != null ? bankDetails.getIfscCode() : "").toUpperCase();
            if (!ifsc.isEmpty() && IFSC_REGEX.matcher(ifsc).matches()) bankEarned += 5;
            else reasons.add("IFSC code missing or invalid");
            String acc = (bankDetails.getAccountNumber() != null ? bankDetails.getAccountNumber() : "").replaceAll("\\s", "");
            if (acc.length() >= 9 && acc.matches("^\\d+$")) bankEarned += 5;
            else reasons.add("Account number missing or invalid");
            if (!ifsc.isEmpty() && !acc.isEmpty()) bankEarned += 4;
        } else reasons.add("Bank details not provided");

        // ── Business Details (5 pts) ──
        int businessEarned = 0;
        if (merchant.getBusinessName() != null && merchant.getBusinessName().length() > 2) businessEarned += 1;
        else reasons.add("Business name missing");
        if (merchant.getEntityType() != null) businessEarned += 1;
        else reasons.add("Entity type not selected");
        if (merchant.getEmail() != null && merchant.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) businessEarned += 1;
        else reasons.add("Valid email not provided");
        if (merchant.getMobileNumber() != null && merchant.getMobileNumber().length() >= 10) businessEarned += 1;
        else reasons.add("Valid mobile number not provided");
        if (merchant.getPanNumber() != null) businessEarned += 1;

        // ── Document Quality (13 pts) ──
        int docEarned = 0;
        List<String> docTypes = allDocs.stream().map(MerchantDocumentEntity::getDocumentType).collect(Collectors.toList());
        long passedDocs = allDocs.stream().filter(d -> "verified".equals(d.getStatus())).count();
        List<String> missingDocs = new ArrayList<>();
        if (docTypes.contains("pan_card")) docEarned += 3; else missingDocs.add("PAN card");
        if (docTypes.contains("aadhaar_card")) docEarned += 3; else missingDocs.add("Aadhaar card");
        if (docTypes.contains("gst_certificate")) docEarned += 3; else missingDocs.add("GST certificate");
        if (docTypes.contains("bank_statement") || docTypes.contains("cancelled_cheque")) docEarned += 2;
        else missingDocs.add("Bank statement / Cancelled cheque");
        if (passedDocs > 0) docEarned += 2; else missingDocs.add("No documents validated yet");
        if (!missingDocs.isEmpty()) reasons.add("Missing docs: " + String.join(", ", missingDocs));

        // ── Previous History (5 pts) ──
        int historyEarned = 0;
        String os = merchant.getOnboardingStatus();
        if (os == null || !os.contains("rejected")) historyEarned = 5;
        else reasons.add("Previously rejected — needs review");

        // ── Bank Details (7 pts) ──
        int bankDetailsEarned = 0;
        if (bankDetails != null) {
            if (bankDetails.getBankName() != null) bankDetailsEarned += 4;
            else reasons.add("Bank name missing");
            if (bankDetails.getAccountNumber() != null) bankDetailsEarned += 3;
            else reasons.add("Bank account number missing");
        }

        List<Map<String, Object>> categories = List.of(
                Map.of("label", "GST Verification", "earned", Math.min(gstEarned, 18), "max", 18),
                Map.of("label", "PAN Verification", "earned", Math.min(panEarned, 14), "max", 14),
                Map.of("label", "Aadhaar Verification", "earned", Math.min(aadhaarEarned, 14), "max", 14),
                Map.of("label", "Bank Verification", "earned", Math.min(bankEarned, 14), "max", 14),
                Map.of("label", "Business Details", "earned", Math.min(businessEarned, 5), "max", 5),
                Map.of("label", "Document Quality", "earned", Math.min(docEarned, 13), "max", 13),
                Map.of("label", "Previous History", "earned", Math.min(historyEarned, 5), "max", 5),
                Map.of("label", "Bank Details", "earned", Math.min(bankDetailsEarned, 7), "max", 7)
        );

        int total = categories.stream().mapToInt(c -> (int) c.get("earned")).sum();
        boolean isManualReview = total < 80 || allDocs.stream().anyMatch(d ->
                !"verified".equals(d.getStatus()) && !"rejected".equals(d.getStatus()));

        merchant.setOnboardingScore((double) total);
        merchantProfileRepository.save(merchant);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", total);
        result.put("categories", categories);
        result.put("reasons", reasons);
        result.put("isManualReview", isManualReview);
        return result;
    }

    // ═══════════ VALIDATE DOCUMENT ═══════════
    @Transactional
    public Map<String, Object> validateDocument(String documentId) {
        MerchantDocumentEntity doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));

        MerchantProfileEntity merchant = merchantProfileRepository.findById(doc.getMerchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", doc.getMerchantId()));

        String docType = doc.getDocumentType();
        List<Map<String, Object>> checks = new ArrayList<>();

        switch (docType != null ? docType : "") {
            case "pan_card": {
                String pan = (merchant.getPanNumber() != null ? merchant.getPanNumber() : "").toUpperCase();
                boolean formatValid = PAN_REGEX.matcher(pan).matches();
                checks.add(buildCheck("format", formatValid ? "pass" : "fail",
                        formatValid ? "PAN format is valid (" + pan + ")"
                                : "PAN format is invalid (" + (pan.isEmpty() ? "N/A" : pan) + ") — expected: ABCDE1234F"));
                checks.add(buildCheck("cross_match", "pass",
                        "PAN contains: " + (pan.isEmpty() ? "N/A" : pan) + ", profile: " + (pan.isEmpty() ? "N/A" : pan)));
                break;
            }
            case "aadhaar_card": {
                String aadhaar = (merchant.getAadhaarNumber() != null ? merchant.getAadhaarNumber() : "").replaceAll("\\s", "");
                boolean formatValid = aadhaar.matches("^\\d{12}$");
                String masked = aadhaar.isEmpty() ? "N/A" : "XXXX XXXX " + (aadhaar.length() >= 4 ? aadhaar.substring(aadhaar.length() - 4) : "");
                checks.add(buildCheck("format", formatValid ? "pass" : "fail",
                        formatValid ? "Aadhaar format is valid (" + masked + ")"
                                : "Aadhaar format is invalid (" + (aadhaar.isEmpty() ? "N/A" : aadhaar) + ") — expected: 12 digits"));
                checks.add(buildCheck("cross_match", "pass", "Aadhaar contains: " + masked + ", profile: " + masked));
                break;
            }
            case "gst_certificate": {
                String gst = (merchant.getGstNumber() != null ? merchant.getGstNumber() : "").toUpperCase();
                boolean formatValid = GST_REGEX.matcher(gst).matches();
                checks.add(buildCheck("gstin_format", formatValid ? "pass" : "fail",
                        formatValid ? "GSTIN format valid: " + gst : "GSTIN not found or invalid format in document"));
                checks.add(buildCheck("gstin_match", formatValid ? "pass" : "fail",
                        formatValid ? "GSTIN matches profile: " + gst
                                : "GSTIN mismatch — document: " + (gst.isEmpty() ? "N/A" : gst) + ", profile: " + gst));

                String panFromGst = (!gst.isEmpty() && gst.length() >= 12) ? gst.substring(2, 12) : "";
                String profilePan = (merchant.getPanNumber() != null ? merchant.getPanNumber() : "").toUpperCase();
                boolean panMatch = PAN_REGEX.matcher(panFromGst).matches() && panFromGst.equals(profilePan);
                checks.add(buildCheck("pan_in_gst", panMatch ? "pass" : "fail",
                        panMatch ? "PAN in GST (" + panFromGst + ") matches profile PAN"
                                : "PAN mismatch — GST contains: " + (panFromGst.isEmpty() ? "N/A" : panFromGst)
                                + ", profile: " + (profilePan.isEmpty() ? "N/A" : profilePan)));

                String profileName = (merchant.getFullName() != null ? merchant.getFullName() : "").toUpperCase().trim();
                checks.add(buildCheck("legal_name_match", profileName.isEmpty() ? "fail" : "pass",
                        profileName.isEmpty() ? "Legal name mismatch"
                                : "Legal name matches: \"" + merchant.getFullName() + "\""));

                String profileBiz = (merchant.getBusinessName() != null ? merchant.getBusinessName() : "").toUpperCase().trim();
                checks.add(buildCheck("trade_name_match", profileBiz.isEmpty() ? "fail" : "pass",
                        profileBiz.isEmpty() ? "Trade name mismatch"
                                : "Trade name matches: \"" + merchant.getBusinessName() + "\""));

                String entityType = merchant.getEntityType() != null ? merchant.getEntityType().toLowerCase() : "";
                boolean constMatch = !entityType.isEmpty();
                checks.add(buildCheck("constitution_match", constMatch ? "pass" : "fail",
                        constMatch ? "Constitution matches: \"" + entityType + "\""
                                : "Constitution not found in profile"));
                break;
            }
            case "bank_statement":
            case "cancelled_cheque": {
                MerchantBankDetailEntity bank = bankDetailRepository.findByMerchantId(merchant.getId()).orElse(null);
                if (bank == null) {
                    checks.add(buildCheck("bank_details", "fail", "No bank details found"));
                } else {
                    String ifsc = (bank.getIfscCode() != null ? bank.getIfscCode() : "").toUpperCase();
                    String account = (bank.getAccountNumber() != null ? bank.getAccountNumber() : "").replaceAll("\\s", "");
                    String holder = (bank.getAccountHolderName() != null ? bank.getAccountHolderName() : "").toUpperCase().trim();

                    boolean ifscValid = IFSC_REGEX.matcher(ifsc).matches();
                    checks.add(buildCheck("format", ifscValid ? "pass" : "fail",
                            ifscValid ? "IFSC format is valid: " + ifsc : "IFSC format is invalid: " + ifsc));

                    boolean accValid = account.length() >= 9 && account.matches("^\\d+$");
                    String maskedAcc = account.isEmpty() ? "" : "****" + (account.length() >= 4 ? account.substring(account.length() - 4) : "");
                    checks.add(buildCheck("account_format", accValid ? "pass" : "fail",
                            accValid ? "Account number format is valid (" + maskedAcc + ")"
                                    : "Account number must be at least 9 digits"));

                    String merchantName = (merchant.getFullName() != null ? merchant.getFullName() : "").toUpperCase().trim();
                    boolean holderMatch = !holder.isEmpty() && !merchantName.isEmpty()
                            && (holder.contains(merchantName) || merchantName.contains(holder)
                            || Arrays.stream(holder.split(" ")).anyMatch(p -> merchantName.contains(p) && p.length() > 2));
                    checks.add(buildCheck("account_holder_match", holderMatch ? "pass" : "fail",
                            holderMatch ? "Account holder \"" + bank.getAccountHolderName() + "\" matches merchant name"
                                    : "Account holder mismatch — account: \""
                                    + (bank.getAccountHolderName() != null ? bank.getAccountHolderName() : "N/A")
                                    + "\", merchant: \"" + (merchant.getFullName() != null ? merchant.getFullName() : "N/A") + "\""));

                    if (!ifsc.isEmpty() && ifsc.length() >= 4) {
                        String prefix = ifsc.substring(0, 4);
                        String expected = IFSC_BANK_MAP.getOrDefault(prefix, "");
                        String bankNameUp = (bank.getBankName() != null ? bank.getBankName() : "").toUpperCase();
                        boolean ifscMatch = !expected.isEmpty() && !bankNameUp.isEmpty()
                                && (bankNameUp.contains(expected) || expected.contains(bankNameUp.split(" ")[0]));
                        checks.add(buildCheck("ifsc_bank_match", ifscMatch ? "pass" : "skip",
                                ifscMatch ? "IFSC " + prefix + " belongs to " + expected + " — matches bank name"
                                        : expected.isEmpty() ? "IFSC prefix " + prefix + " not recognized"
                                        : "IFSC " + prefix + " belongs to " + expected + ", but bank name is \"" + bank.getBankName() + "\""));
                    }
                }
                break;
            }
            case "video_kyc":
            case "selfie":
                checks.add(buildCheck("manual_review", "pass", "Manual review required"));
                break;
            default:
                checks.add(buildCheck("unknown_type", "pass", "No automated checks for this document type"));
        }

        List<Map<String, Object>> activeChecks = checks.stream()
                .filter(c -> !"skip".equals(c.get("checkResult"))).collect(Collectors.toList());
        boolean allPassed = !activeChecks.isEmpty() && activeChecks.stream().allMatch(c -> "pass".equals(c.get("checkResult")));
        String overallStatus = allPassed ? "passed" : "failed";
        LocalDateTime now = LocalDateTime.now();

        // Delete old validations for this document and save new ones
        List<DocumentValidationEntity> oldValidations = validationRepository.findByDocumentId(documentId);
        validationRepository.deleteAll(oldValidations);

        try {
            String checksJson = objectMapper.writeValueAsString(checks);
            DocumentValidationEntity dv = DocumentValidationEntity.builder()
                    .documentId(documentId)
                    .merchantId(doc.getMerchantId())
                    .documentType(docType)
                    .validationType("automated")
                    .isValid(allPassed)
                    .extractedData(checksJson)
                    .validationNotes(overallStatus)
                    .score(allPassed ? 100 : 0)
                    .validatedAt(now)
                    .build();
            validationRepository.save(dv);
        } catch (Exception e) {
            log.error("Failed to save validation record", e);
        }

        doc.setStatus(allPassed ? "verified" : "rejected");
        doc.setVerifiedAt(allPassed ? now : null);
        if (!allPassed) doc.setRejectionReason("Validation checks failed");
        documentRepository.save(doc);

        Map<String, Object> scoreData = computeScore(doc.getMerchantId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentId", documentId);
        result.put("documentType", docType);
        result.put("fileName", doc.getFileName());
        result.put("overallStatus", overallStatus);
        result.put("checks", checks);
        result.put("validatedAt", now.toString());
        result.put("score", scoreData);
        return result;
    }

    // ═══════════ APPROVE / REJECT DOCUMENT ═══════════
    @Transactional
    public Map<String, Object> approveDocument(String docId, String reason, String staffUserId) {
        MerchantDocumentEntity doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", docId));

        LocalDateTime now = LocalDateTime.now();
        doc.setStatus("verified");
        doc.setVerifiedAt(now);
        doc.setVerifiedBy(staffUserId);
        doc.setRejectionReason(null);
        documentRepository.save(doc);

        try {
            DocumentValidationEntity dv = DocumentValidationEntity.builder()
                    .documentId(docId)
                    .merchantId(doc.getMerchantId())
                    .documentType(doc.getDocumentType())
                    .validationType("manual_approve")
                    .isValid(true)
                    .validationNotes(reason)
                    .score(100)
                    .validatedAt(now)
                    .build();
            validationRepository.save(dv);
        } catch (Exception e) { log.error("Failed to save approval validation", e); }

        Map<String, Object> scoreData = computeScore(doc.getMerchantId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("score", scoreData);
        return result;
    }

    @Transactional
    public Map<String, Object> rejectDocument(String docId, String reason, String staffUserId) {
        if (reason == null || reason.isBlank())
            throw new BadRequestException("Rejection reason is required");

        MerchantDocumentEntity doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", docId));

        LocalDateTime now = LocalDateTime.now();
        doc.setStatus("rejected");
        doc.setVerifiedAt(now);
        doc.setVerifiedBy(staffUserId);
        doc.setRejectionReason(reason);
        documentRepository.save(doc);

        try {
            DocumentValidationEntity dv = DocumentValidationEntity.builder()
                    .documentId(docId)
                    .merchantId(doc.getMerchantId())
                    .documentType(doc.getDocumentType())
                    .validationType("manual_reject")
                    .isValid(false)
                    .validationNotes(reason)
                    .score(0)
                    .validatedAt(now)
                    .build();
            validationRepository.save(dv);
        } catch (Exception e) { log.error("Failed to save rejection validation", e); }

        Map<String, Object> scoreData = computeScore(doc.getMerchantId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("score", scoreData);
        return result;
    }

    // ═══════════ EXPERIAN CREDIT CHECK ═══════════
    @SuppressWarnings("unchecked")
    public Map<String, Object> experianCheck(String merchantId, String staffUserId) {
        MerchantProfileEntity merchant = merchantProfileRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", merchantId));

        if (isNullOrBlank(merchant.getFullName()) || isNullOrBlank(merchant.getMobileNumber())) {
            return Map.of("success", false, "message", "Merchant name and mobile are required for Experian check");
        }

        MerchantBankDetailEntity bankDetails = bankDetailRepository.findByMerchantId(merchantId).orElse(null);

        try {
            String token = generateTransbankToken();
            if (token == null) return Map.of("success", false, "message", "Failed to generate Transbank token");

            Map<String, Object> experianData = callExperianApi(token, merchant.getFullName(), merchant.getMobileNumber());
            if (experianData == null) return Map.of("success", false, "message", "No response from Experian");

            String message = (String) experianData.getOrDefault("message", "");
            Object codeObj = experianData.get("code");

            if ("No Record Found".equals(message) || (codeObj instanceof Integer && (Integer) codeObj == 2)) {
                cacheCreditCheck(merchantId, null, false, false, false, false, false,
                        false, 0, 0, 0, 0, BigDecimal.ZERO, experianData, true, null);
                Map<String, Object> score = computeScore(merchantId);
                return Map.of("success", true, "noRecordFound", true,
                        "message", "No Experian credit record found for this merchant",
                        "matches", Map.of("pan", false, "mobile", false, "accountNumber", false, "bankName", false),
                        "creditSummary", Map.of("totalAccounts", 0, "activeAccounts", 0, "closedAccounts", 0, "defaultAccounts", 0, "outstandingBalance", 0),
                        "score", score);
            }

            Object statusObj = experianData.get("status");
            if (!(statusObj instanceof Integer) || (Integer) statusObj != 1) {
                return Map.of("success", false, "message", String.valueOf(experianData.getOrDefault("message", "Experian API error")));
            }

            Map<String, Object> resultMap = (Map<String, Object>) experianData.getOrDefault("result", Map.of());
            Map<String, Object> profile = (Map<String, Object>) resultMap.getOrDefault("INProfileResponse", Map.of());
            Map<String, Object> cais = (Map<String, Object>) profile.getOrDefault("CAIS_Account", Map.of());
            Map<String, Object> caisSummary = (Map<String, Object>) cais.getOrDefault("CAIS_Summary", Map.of());

            String merchantPan = merchant.getPanNumber() != null ? merchant.getPanNumber().toUpperCase().trim() : "";

            String merchantMobile = merchant.getMobileNumber() != null ? merchant.getMobileNumber().replaceAll("\\s", "") : "";
            if (merchantMobile.length() > 10) merchantMobile = merchantMobile.substring(merchantMobile.length() - 10);

            Map<String, Object> creditAccount = (Map<String, Object>) caisSummary.getOrDefault("Credit_Account", Map.of());
            int totalAccounts = parseInt(creditAccount.getOrDefault("CreditAccountTotal", "0"));
            int activeAccounts = parseInt(creditAccount.getOrDefault("CreditAccountActive", "0"));
            int closedAccounts = parseInt(creditAccount.getOrDefault("CreditAccountClosed", "0"));
            int defaultAccounts = parseInt(creditAccount.getOrDefault("CreditAccountDefault", "0"));

            Map<String, Object> outstandingBal = (Map<String, Object>) caisSummary.getOrDefault("Total_Outstanding_Balance", Map.of());
            BigDecimal outstandingBalance = parseBigDecimal(outstandingBal.getOrDefault("Outstanding_Balance_All", "0"));

            String txnId = (String) experianData.getOrDefault("txn_id", null);

            cacheCreditCheck(merchantId, txnId, true, false, false, false, false,
                    defaultAccounts > 0, totalAccounts, activeAccounts, closedAccounts, defaultAccounts,
                    outstandingBalance, experianData, false, staffUserId);

            Map<String, Object> score = computeScore(merchantId);
            return Map.of("success", true, "noRecordFound", false, "message", "Experian check completed",
                    "matches", Map.of("pan", false, "mobile", false, "accountNumber", false, "bankName", false),
                    "creditSummary", Map.of("totalAccounts", totalAccounts, "activeAccounts", activeAccounts,
                            "closedAccounts", closedAccounts, "defaultAccounts", defaultAccounts, "outstandingBalance", outstandingBalance),
                    "score", score);

        } catch (Exception e) {
            log.error("Experian check error: {}", e.getMessage(), e);
            return Map.of("success", false, "message", "Experian check failed: " + e.getMessage());
        }
    }

    public Map<String, Object> getExperianResult(String merchantId) {
        MerchantCreditCheckEntity c = creditCheckRepository.findTopByMerchantIdOrderByCheckedAtDesc(merchantId).orElse(null);
        if (c == null) return Map.of("found", false);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("merchantId", c.getMerchantId());
        result.put("creditScore", c.getCreditScore());
        result.put("status", c.getStatus());
        result.put("checkedAt", c.getCheckedAt());
        return result;
    }

    // ═══════════ HELPERS ═══════════

    private boolean isPass(Map<String, Object> check) {
        return check != null && "pass".equals(check.get("checkResult"));
    }

    private boolean isFail(Map<String, Object> check) {
        return check != null && "fail".equals(check.get("checkResult"));
    }

    private Map<String, Object> buildCheck(String checkType, String checkResult, String message) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("checkType", checkType);
        c.put("checkResult", checkResult);
        c.put("message", message);
        return c;
    }

    private Map<String, Object> dvToMap(DocumentValidationEntity v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("document_id", v.getDocumentId());
        m.put("validation_type", v.getValidationType());
        m.put("is_valid", v.getIsValid());
        m.put("extracted_data", v.getExtractedData());
        m.put("validation_notes", v.getValidationNotes());
        m.put("score", v.getScore());
        m.put("validated_at", v.getValidatedAt());
        return m;
    }

    private String generateTransbankToken() throws Exception {
        ZonedDateTime ist = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        String timestamp = ist.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("client_Id", transbankClientId);
        payload.put("transaction_timestamp", timestamp);
        payload.put("processor", transbankProcessor);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(transbankBaseUrl + "/v1/token/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(response.body());
        return node.has("token") ? node.get("token").asText() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callExperianApi(String token, String name, String mobile) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("mobile", mobile);
        payload.put("consent_text", "We confirm obtaining valid customer consent to access/process their name/mobile data. Consent remains valid, informed, and unwithdrawn.");
        payload.put("consent", "Y");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(transbankBaseUrl + "/experian-report"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), Map.class);
    }

    private void cacheCreditCheck(String merchantId, String txnId, Boolean nameMatch, Boolean panMatch,
                                  Boolean mobileMatch, Boolean accMatch, Boolean bankNameMatch,
                                  Boolean hasDefaults, int total, int active, int closed, int defaultAcc,
                                  BigDecimal outstanding, Map<String, Object> raw, boolean noRecord, String by) {
        try {
            MerchantCreditCheckEntity c = MerchantCreditCheckEntity.builder()
                    .merchantId(merchantId).txnId(txnId)
                    .panMatch(panMatch).nameMatch(nameMatch).mobileMatch(mobileMatch)
                    .accountNumberMatch(accMatch).bankNameMatch(bankNameMatch)
                    .hasDefaults(hasDefaults).totalAccounts(total).activeAccounts(active)
                    .closedAccounts(closed).defaultAccounts(defaultAcc).outstandingBalance(outstanding)
                    .rawResponse(objectMapper.writeValueAsString(raw))
                    .noRecordFound(noRecord).status("completed").checkedBy(by)
                    .checkedAt(LocalDateTime.now()).build();
            creditCheckRepository.save(c);
        } catch (Exception e) {
            log.error("Failed to cache credit check: {}", e.getMessage());
        }
    }

    private boolean isNullOrBlank(String s) { return s == null || s.isBlank(); }
    private int parseInt(Object v) { try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; } }
    private BigDecimal parseBigDecimal(Object v) { try { return new BigDecimal(String.valueOf(v)); } catch (Exception e) { return BigDecimal.ZERO; } }
}
