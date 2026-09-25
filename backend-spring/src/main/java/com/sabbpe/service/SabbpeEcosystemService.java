package com.sabbpe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabbpe.dto.EcosystemMandateCreateRequest;
import com.sabbpe.dto.EcosystemMandateCreateResponse;
import com.sabbpe.dto.EcosystemMandateStatusResponse;
import com.sabbpe.dto.EcosystemMandateWebhookRequest;
import com.sabbpe.exception.BadGatewayException;
import com.sabbpe.model.MerchantProfileEntity;
import com.sabbpe.model.ProductCatalogEntity;
import com.sabbpe.repository.MerchantProfileRepository;
import com.sabbpe.repository.ProductCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client for the Onboarding Team's 5 ecosystem APIs (ecosystemuat.sabbpe.com),
 * per
 * ONBOARDING_TEAM_GUIDE.md: merchant onboard -> token -> mandate create ->
 * mandate status
 * -> first product subscription. Replaces the old direct-to-CAMS call in the
 * frontend
 * (MandateCreate.tsx) so the secret_key and NACH_MANDATE service credentials
 * never leave
 * this server.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SabbpeEcosystemService {

    private final RestTemplate restTemplate;
    private final MerchantProfileRepository merchantProfileRepository;
    private final ProductCatalogRepository productCatalogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ecosystem.base-url}")
    private String baseUrl;

    @Value("${app.ecosystem.secret-key}")
    private String secretKey;

    @Value("${app.ecosystem.organization-code-prefix}")
    private String organizationCodePrefix;

    @Value("${app.ecosystem.nach-mandate.sabbpe-userid}")
    private String nachUserId;

    @Value("${app.ecosystem.nach-mandate.sabbpe-password}")
    private String nachPassword;

    @Value("${app.ecosystem.nach-mandate.sabbpe-merchantid}")
    private String nachMerchantId;

    @Value("${app.ecosystem.upi-register-callback-url}")
    private String upiRegisterCallbackUrl;

    @Value("${app.ecosystem.upi-debit-url}")
    private String upiDebitUrl;

    @Value("${app.ecosystem.webhook-secret}")
    private String webhookSecret;

    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter TOKEN_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String NACH_MANDATE_SERVICE = "NACH_MANDATE";

    // ── Step 1: onboard the merchant into the ecosystem (once, lazily, idempotent)
    // ──

    @Transactional
    public MerchantProfileEntity ensureOnboarded(MerchantProfileEntity merchant) {
        if (merchant.getEcosystemOrganizationId() != null) {
            return merchant;
        }

        String shortId = merchant.getId().replace("-", "").toUpperCase();
        String organizationId = "ORG-" + shortId.substring(0, Math.min(12, shortId.length()));
        String organizationCode = organizationCodePrefix + shortId.substring(0, Math.min(8, shortId.length()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("secret_key", secretKey);
        body.put("organization_id", organizationId);
        body.put("organization_code", organizationCode);
        String name = merchant.getBusinessName() != null ? merchant.getBusinessName() : merchant.getFullName();
        body.put("legal_name", name);
        body.put("trade_name", name);
        body.put("email", merchant.getEmail());
        body.put("mobile", merchant.getMobileNumber());
        body.put("gst_number", merchant.getGstNumber());
        body.put("pan_number", merchant.getPanNumber());
        body.put("country_code", "IN");
        body.put("currency_code", "INR");
        body.put("timezone_name", "Asia/Kolkata");
        body.put("services", onboardServices(merchant));

        JsonNode response = post("/sabbpe/v1/merchant/onboard", body, null);
        if (response == null || !"success".equalsIgnoreCase(textOf(response, "status"))) {
            throw new BadGatewayException("Ecosystem onboarding failed: " + errorMessage(response));
        }

        merchant.setEcosystemOrganizationId(organizationId);
        merchant.setEcosystemOrganizationCode(organizationCode);
        merchant.setEcosystemOnboardedAt(LocalDateTime.now());
        return merchantProfileRepository.save(merchant);
    }

    // Product catalog code -> ecosystem service code. Only products that actually
    // correspond
    // to one of the 5 valid ecosystem services are listed; lending/current-account
    // products
    // have no ecosystem service and are intentionally omitted.
    private static final Map<String, String> PRODUCT_TO_SERVICE = Map.of(
            "PROD_001", "PAYMENT_GATEWAY", // UPI QR
            "PROD_002", "PAYMENT_GATEWAY", // UPI QR + Soundbox
            "PROD_003", "PAYMENT_GATEWAY", // POS Terminal
            "PROD_004", "PAYMENT_GATEWAY", // Payment Gateway
            "PROD_006", "PAYMENT_GATEWAY", // Gift Vouchers (sold via payment collection)
            "PROD_007", "PAYOUT", // Payout
            "PROD_010", "KYC" // KYC APIs
    );

    private List<String> onboardServices(MerchantProfileEntity merchant) {
        // KYC + NACH_MANDATE are non-negotiable — our own token/mandate-create calls
        // (getToken(..., NACH_MANDATE_SERVICE)) depend on this org having that service
        // active.
        // Removing it breaks mandate creation for this merchant entirely.
        Set<String> services = new LinkedHashSet<>();
        services.add("KYC");
        services.add(NACH_MANDATE_SERVICE);
        for (String productCode : selectedProductCodes(merchant)) {
            String serviceCode = PRODUCT_TO_SERVICE.get(productCode);
            if (serviceCode != null) {
                services.add(serviceCode);
            }
            // Also include the merchant's actual product name itself, per explicit request
            // 2026-09-23 — even though the ecosystem's guide says "service codes only,
            // never
            // provider names", so this may not generate real credentials for the product
            // name
            // entries the way it does for the 5 recognized service codes above.
            String productName = productCatalogRepository.findByProductCode(productCode)
                    .map(ProductCatalogEntity::getProductName)
                    .orElse(productCode);
            services.add(productName);
        }
        return new ArrayList<>(services);
    }

    // ── Step 2: token (fetched fresh per call; valid 15 min, cheap enough not to
    // cache) ──

    private String getToken(String merchantOrderRef, String serviceCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sabbpe_userid", nachUserId);
        body.put("sabbpe_merchantid", nachMerchantId);
        body.put("sabbpe_password", nachPassword);
        body.put("timestamp", LocalDateTime.now(ZoneId.of("Asia/Kolkata")).format(TOKEN_TIMESTAMP));
        body.put("merchant_order_ref", merchantOrderRef);
        body.put("service_code", serviceCode);

        JsonNode response = post("/sabbpe/v1/token", body, null);
        if (response == null || !response.path("status").asBoolean(false)) {
            throw new BadGatewayException("Failed to obtain ecosystem token: " + errorMessage(response));
        }
        String token = textOf(response, "sabbpe_token");
        if (token == null) {
            throw new BadGatewayException("Ecosystem token response missing sabbpe_token");
        }
        return token;
    }

    public JsonNode validateVpa(String vpa) {
        String token = getToken("ORD-VPA-" + System.currentTimeMillis(), NACH_MANDATE_SERVICE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sabbpe_token", token);
        body.put("vpa", vpa);
        return post("/api/v1/validvpa", body, null);
    }

    // ── Step 3: create the UPI AutoPay mandate ──

    // Deliberately NOT @Transactional: if the mandate-create call below fails, we
    // still want
    // ensureOnboarded()'s save (the org id the ecosystem just issued us) to have
    // committed —
    // wrapping this whole method in one transaction was rolling that back on every
    // failure,
    // so every retry re-onboarded as a brand-new organization instead of reusing
    // the same one.
    public EcosystemMandateCreateResponse createUpiMandate(MerchantProfileEntity merchant,
            EcosystemMandateCreateRequest request) {
        merchant = ensureOnboarded(merchant);
        String token = getToken("ORD-MDT-" + System.currentTimeMillis(), NACH_MANDATE_SERVICE);

        // CAMS hard-rejects trxnno over 35 chars (our old "UPI-<org id>-<timestamp>"
        // format
        // could run well past that) — per the mandate team's working contract,
        // 2026-09-22:
        // "UPIAUTOPAY-" + 16 hex chars = 27 total, never embed the org id in it.
        String trxnno = "UPIAUTOPAY-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        LocalDate start = LocalDate.parse(request.getStartDate());
        LocalDate end = LocalDate.parse(request.getEndDate());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sabbpe_token", token);
        // Internal correlation only — stored on their mandate row, stripped before
        // CAMS.
        body.put("merchant_organization_id", merchant.getEcosystemOrganizationId());
        body.put("trxnno", trxnno);
        body.put("amount", request.getAmount());
        body.put("pattern", "ASPRESENTED");
        body.put("mandatestartdate", start.format(API_DATE));
        body.put("mandateenddate", end.format(API_DATE));
        body.put("revokeable", "Y");
        body.put("payervpa", request.getVpa());
        body.put("payername", request.getPayerName());
        body.put("redirecturl", upiRegisterCallbackUrl);
        body.put("debiturl", upiDebitUrl);
        body.put("executabledays", String.format("%02d", start.getDayOfMonth()));
        body.put("executablemonth", String.format("%02d", start.getMonthValue()));
        body.put("authorize", "N");
        body.put("authorizerevoke", "Y");
        // instaauth=N (mandate only, no bundled first debit) — instaamount would only
        // be
        // required if this were "Y" (per the mandate team's flow doc, 2026-09-23).
        body.put("instaauth", "N");
        body.put("instaamount", "");
        body.put("intent", "N");
        body.put("mandateexpirytime", 120);

        // Success has NO "redirect" field — CAMS pushes the collect request straight to
        // the
        // payer's UPI app instead of a browser page. Success looks like:
        // {"status":"PENDING","errCode":"1111","errDesc":"Please
        // authorise...","cp_mdt_ref_no":"…"}
        // (confirmed with the mandate team 2026-09-22, after their guide's "redirect"
        // example
        // turned out to only apply to the eNACH flow, not UPI AutoPay).
        JsonNode response = post("/api/v1/mandatecreate", body, token);
        boolean accepted = response != null
                && "1111".equals(textOf(response, "errCode"))
                && "PENDING".equalsIgnoreCase(textOf(response, "status"));
        if (!accepted) {
            throw new BadGatewayException("Mandate creation failed: " + errorMessage(response));
        }

        String camsReference = textOf(response, "cp_mdt_ref_no");
        merchant.setUpiMandateRefNo(trxnno);
        merchant.setEcosystemCamsReference(camsReference);
        merchant.setUpiMandateStatus("initiated");
        merchant.setEcosystemMandateAnchorDay(start.getDayOfMonth());
        merchantProfileRepository.save(merchant);

        return EcosystemMandateCreateResponse.builder()
                .trxnno(trxnno)
                .message(textOf(response, "errDesc"))
                .camsReference(camsReference)
                .build();
    }

    // ── Step 4 + 5: poll mandate status (recovery fallback — the webhook below is
    // primary);
    // once active, create the first product's subscription ──

    @Transactional
    public EcosystemMandateStatusResponse pollStatus(MerchantProfileEntity merchant, String trxnno) {
        // Structured endpoint per ONBOARDING_INTEGRATION_GUIDE.md §11.5 (hyphenated
        // path,
        // richer JSON than the old plain-string /api/v1/ob/mandatestatus). Must query
        // by the
        // CAMS-assigned cp_mdt_ref_no, NOT our own trxnno — querying by trxnno always
        // returns
        // NOT_FOUND (confirmed 2026-09-23). Fall back to trxnno only if we never
        // captured one
        // (shouldn't happen for any mandate created after this fix).
        String ref = merchant.getEcosystemCamsReference() != null ? merchant.getEcosystemCamsReference() : trxnno;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ref", ref);
        body.put("merchant_organization_id", merchant.getEcosystemOrganizationId());
        JsonNode response = post("/api/v1/ob/mandate-status", body, null);
        String normalized = normalizeStatus(textOf(response, "mandate_status"));

        merchant = applyMandateStatus(merchant, normalized);

        return EcosystemMandateStatusResponse.builder()
                .status(normalized)
                .subscriptionId(merchant.getEcosystemSubscriptionId())
                .nextDueDate(merchant.getEcosystemSubscriptionNextDueDate())
                .build();
    }

    // ── Webhook: ecosystem's primary, asynchronous mandate-status notification
    // (§11.4) ──

    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            // Matches their documented behavior: no secret configured yet -> they send the
            // webhook without a signature header, so we can't verify it either.
            return true;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(rawBody.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hmacBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString().equalsIgnoreCase(signatureHeader.trim());
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }

    @Transactional
    public void handleStatusWebhook(String rawBody) {
        EcosystemMandateWebhookRequest webhook;
        try {
            webhook = objectMapper.readValue(rawBody, EcosystemMandateWebhookRequest.class);
        } catch (Exception e) {
            log.error("Failed to parse mandate status webhook body: {}", e.getMessage());
            return;
        }

        if (webhook.getMerchantOrganizationId() == null) {
            log.warn("Mandate status webhook missing merchant_organization_id, ignoring: {}", rawBody);
            return;
        }

        MerchantProfileEntity merchant = merchantProfileRepository
                .findByEcosystemOrganizationId(webhook.getMerchantOrganizationId())
                .orElse(null);
        if (merchant == null) {
            log.warn("Mandate status webhook for unknown organization {}", webhook.getMerchantOrganizationId());
            return;
        }

        log.info("Mandate status webhook: org={}, mandateRef={}, status={}",
                webhook.getMerchantOrganizationId(), webhook.getMandateReference(), webhook.getMandateStatus());

        String normalized = normalizeStatus(webhook.getMandateStatus());
        applyMandateStatus(merchant, normalized);
    }

    private MerchantProfileEntity applyMandateStatus(MerchantProfileEntity merchant, String normalizedStatus) {
        merchant.setUpiMandateStatus(normalizedStatus);
        merchant = merchantProfileRepository.save(merchant);

        if ("active".equals(normalizedStatus) && merchant.getEcosystemSubscriptionId() == null) {
            merchant = createFirstSubscription(merchant);
        }
        return merchant;
    }

    private String normalizeStatus(String raw) {
        if (raw == null)
            return "pending";
        String upper = raw.trim().toUpperCase();
        if (upper.contains("ACTIVE"))
            return "active";
        if (upper.contains("FAILED") || upper.contains("REJECT") || upper.contains("CANCEL")
                || upper.contains("REVOKE"))
            return "failed";
        return "pending";
    }

    private MerchantProfileEntity createFirstSubscription(MerchantProfileEntity merchant) {
        String productCode = firstSelectedProductCode(merchant);
        if (productCode == null) {
            log.warn("Mandate for merchant {} went active but no product is selected; skipping subscription",
                    merchant.getId());
            return merchant;
        }

        String productName = productCatalogRepository.findByProductCode(productCode)
                .map(ProductCatalogEntity::getProductName)
                .orElse(productCode);

        BigDecimal amount = merchant.getTotalMonthlyCost() != null
                && merchant.getTotalMonthlyCost().compareTo(BigDecimal.ZERO) > 0
                        ? merchant.getTotalMonthlyCost()
                        : BigDecimal.ONE;

        String token = getToken("ORD-SUB-" + System.currentTimeMillis(), NACH_MANDATE_SERVICE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sabbpe_token", token);
        // Not shown in their documented example, but required in practice — without it
        // the
        // call resolves to some other/default organization instead of this merchant's
        // (confirmed 2026-09-22: omitting it produced "No ACTIVE UPI mandate for
        // organization
        // b3718c34-..." — not this merchant's org id at all).
        body.put("merchant_organization_id", merchant.getEcosystemOrganizationId());
        body.put("product_code", productCode);
        body.put("product_name", productName);
        body.put("amount", amount);
        // Match the mandate's own recurring day (its executabledays at creation), not
        // whichever day the status poll happened to land on after the merchant
        // authorized it.
        int anchorDay = merchant.getEcosystemMandateAnchorDay() != null
                ? merchant.getEcosystemMandateAnchorDay()
                : LocalDate.now().getDayOfMonth();
        body.put("anchor_day", anchorDay);
        body.put("source", "ONBOARDING");
        body.put("mandate_type", "UPI");

        JsonNode response = post("/api/v1/subscriptions", body, token);
        if (response == null || !"success".equalsIgnoreCase(textOf(response, "status"))) {
            log.error("Subscription creation failed for merchant {}: {}", merchant.getId(), errorMessage(response));
            return merchant;
        }

        merchant.setEcosystemSubscriptionId(textOf(response, "subscription_id"));
        merchant.setEcosystemSubscriptionNextDueDate(textOf(response, "next_due_date"));
        return merchantProfileRepository.save(merchant);
    }

    // ── Integration-fee payment status (handoff §5, POST
    // /sabbpe/v1/merchant/payment-status) ──

    /**
     * Tells the ecosystem the merchant paid the integration fee, so it records a
     * "payment"
     * object under each paid service in organization.providers. Idempotent on
     * transaction_id
     * on their side, so retries are safe. Never throws: a failure here must not
     * undo the
     * payment already recorded in merchant_profiles.
     */
    public void reportIntegrationFeePaid(MerchantProfileEntity merchant, JsonNode payment) {
        String organizationId = merchant.getEcosystemOrganizationId();
        if (organizationId == null || organizationId.isBlank()) {
            log.warn("Merchant {} has no ecosystem_organization_id; payment-status not reported", merchant.getId());
            return;
        }
        // The payment goes under the product entries onboarding created in
        // providers.services
        // (e.g. "UPI QR + Soundbox"), not under the service codes like PAYMENT_GATEWAY.
        Set<String> services = integrationFeeProductNames(merchant);
        if (services.isEmpty()) {
            log.warn("Merchant {} has no integration-priced products selected; payment-status not reported",
                    merchant.getId());
            return;
        }

        Map<String, Object> paymentBody = new LinkedHashMap<>();
        paymentBody.put("status", "PAID");
        paymentBody.put("amount", formatAmount(textOf(payment, "amount")));
        paymentBody.put("currency", firstNonBlank(textOf(payment, "currency"), "INR"));
        paymentBody.put("payment_date", firstNonBlank(textOf(payment, "payment_completed_at"),
                LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))));
        paymentBody.put("payment_mode", firstNonBlank(textOf(payment, "payment_method"),
                textOf(payment, "payment_mode"), textOf(payment, "mode")));
        paymentBody.put("gateway", firstNonBlank(textOf(payment, "gateway"), "SABBPE"));
        paymentBody.put("transaction_id", firstNonBlank(textOf(payment, "master_transaction_id"),
                textOf(payment, "transaction_id"), merchant.getTransactionId()));
        paymentBody.put("merchant_order_ref", textOf(payment, "merchant_order_ref"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("secret_key", secretKey);
        body.put("organization_id", organizationId);
        body.put("services", new ArrayList<>(services));
        body.put("payment", paymentBody);

        JsonNode response = post("/sabbpe/v1/merchant/payment-status", body, null);
        if (response != null && "success".equalsIgnoreCase(textOf(response, "status"))) {
            log.info("Reported integration-fee payment for {}: updated {}, skipped {}",
                    organizationId, response.path("updated_services"), response.path("skipped_services"));
        } else {
            log.error("Ecosystem payment-status failed for {}: {}", organizationId, errorMessage(response));
        }
    }

    private static String formatAmount(String amount) {
        if (amount == null || amount.isBlank())
            return null;
        try {
            return new BigDecimal(amount.trim()).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException e) {
            return amount;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank())
                return value;
        }
        return null;
    }

    /**
     * Catalog names of the selected products that make up the integration fee
     * (pricing_type "integration", same rule as
     * ProductService.calculateIntegrationCost).
     * Uses the catalog name so it matches the key onboardServices() created in
     * providers.services.
     */
    private Set<String> integrationFeeProductNames(MerchantProfileEntity merchant) {
        String json = merchant.getSelectedProducts();
        if (json == null || json.isBlank())
            return Set.of();
        Set<String> names = new LinkedHashSet<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray())
                return Set.of();
            for (JsonNode item : root) {
                if (!"integration".equals(item.path("pricing_type").asText()))
                    continue;
                String productCode = firstNonBlank(textOf(item, "product_code"), textOf(item, "productCode"));
                if (productCode == null)
                    continue;
                names.add(productCatalogRepository.findByProductCode(productCode)
                        .map(ProductCatalogEntity::getProductName)
                        .orElse(productCode));
            }
        } catch (Exception e) {
            log.warn("Failed to parse selected_products for merchant {}: {}", merchant.getId(), e.getMessage());
        }
        return names;
    }

    private String firstSelectedProductCode(MerchantProfileEntity merchant) {
        List<String> codes = selectedProductCodes(merchant);
        return codes.isEmpty() ? null : codes.get(0);
    }

    private List<String> selectedProductCodes(MerchantProfileEntity merchant) {
        String json = merchant.getSelectedProducts();
        if (json == null || json.isBlank())
            return List.of();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray())
                return List.of();
            List<String> codes = new ArrayList<>();
            for (JsonNode item : root) {
                JsonNode code = item.has("product_code") ? item.get("product_code") : item.get("productCode");
                if (code != null && !code.isNull()) {
                    codes.add(code.asText());
                }
            }
            return codes;
        } catch (Exception e) {
            log.warn("Failed to parse selected_products for merchant {}: {}", merchant.getId(), e.getMessage());
            return List.of();
        }
    }

    // ── HTTP helpers ──

    private JsonNode post(String path, Map<String, Object> body, String bearerToken) {
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + path, entityFor(body, bearerToken),
                    JsonNode.class);
            return response.getBody();
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // The ecosystem often returns a real, informative error body on non-2xx
            // statuses
            // (e.g. CAMSPAY_INVALID_RESPONSE) — surface it to the caller instead of just
            // logging it and returning null, so the API response itself shows the real
            // upstream error rather than a generic "no response" message.
            log.error("Ecosystem call to {} failed: {} {}", path, e.getStatusCode(), e.getResponseBodyAsString());
            try {
                return objectMapper.readTree(e.getResponseBodyAsString());
            } catch (Exception parseError) {
                return null;
            }
        } catch (Exception e) {
            log.error("Ecosystem call to {} failed: {}", path, e.getMessage());
            return null;
        }
    }

    private HttpEntity<Map<String, Object>> entityFor(Map<String, Object> body, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.set("X-Sabbpe-Token", bearerToken);
        }
        return new HttpEntity<>(body, headers);
    }

    private static String textOf(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private static String errorMessage(JsonNode response) {
        if (response == null)
            return "no response from ecosystem service";
        // "msg" holds the real CAMS validation error on a mandatecreate rejection (e.g.
        // "Transaction number(trxno) max length should be 35") — check it first.
        String message = textOf(response, "msg");
        if (message == null)
            message = textOf(response, "message");
        if (message == null)
            message = textOf(response, "errDesc");
        String code = textOf(response, "errorCode");
        if (message == null)
            return response.toString();
        return code != null ? code + ": " + message : message;
    }
}
