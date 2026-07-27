package com.sabbpe.controller;

import com.sabbpe.dto.ApiResponse;
import com.sabbpe.service.WebhookAuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BankWebhookController {

    private final WebhookAuthService webhookAuthService;
    private final ObjectMapper objectMapper;

    @PostMapping("/api/webhooks/bank")
    public ResponseEntity<ApiResponse<Void>> handleBankWebhook(
            HttpServletRequest request,
            @RequestBody Map<String, Object> payload) {

        String signature = request.getHeader("x-webhook-signature");
        String apiKey = request.getHeader("x-api-key");
        String timestamp = request.getHeader("x-webhook-timestamp");

        if (apiKey != null && !webhookAuthService.verifyApiKey(apiKey)) {
            log.warn("Bank webhook rejected: invalid API key");
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "Invalid API key"));
        }

        if (signature != null && timestamp != null) {
            try {
                String payloadStr = objectMapper.writeValueAsString(payload);
                if (!webhookAuthService.verifySignature(signature, timestamp, payloadStr)) {
                    log.warn("Bank webhook rejected: invalid signature");
                    return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "Invalid signature"));
                }
            } catch (Exception e) {
                log.error("Error verifying bank webhook signature", e);
            }
        }

        log.info("Bank webhook received: {}", payload);

        String applicationId = payload.get("applicationId") != null
                ? payload.get("applicationId").toString() : null;
        String status = payload.get("status") != null
                ? payload.get("status").toString() : null;

        if (applicationId != null && status != null) {
            log.info("Bank application {} status updated to {}", applicationId, status);
        }

        return ResponseEntity.ok(ApiResponse.success("Webhook received", null));
    }

    @PostMapping("/api/webhooks/test")
    public ResponseEntity<ApiResponse<Void>> testWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Test webhook received: {}", payload);
        return ResponseEntity.ok(ApiResponse.success("Webhook received", null));
    }

    @PostMapping("/api/supabase/merchant-webhook")
    public ResponseEntity<ApiResponse<Void>> handleSupabaseMerchantWebhook(
            @RequestBody Map<String, Object> payload) {
        log.info("Supabase merchant webhook received: {}", payload);
        return ResponseEntity.ok(ApiResponse.success("Webhook received", null));
    }

    @PostMapping("/api/supabase/bank-webhook")
    public ResponseEntity<ApiResponse<Void>> handleSupabaseBankWebhook(
            @RequestBody Map<String, Object> payload) {
        log.info("Supabase bank webhook received: {}", payload);
        return ResponseEntity.ok(ApiResponse.success("Webhook received", null));
    }
}
