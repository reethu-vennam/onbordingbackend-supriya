package com.sabbpe.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class WebhookAuthService {

    @Value("${app.webhook.secret:}")
    private String webhookSecret;

    @Value("${app.webhook.api-key:}")
    private String webhookApiKey;

    public boolean verifySignature(String signatureHeader, String timestamp, String payload) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Webhook secret not configured, skipping signature verification");
            return true;
        }
        try {
            String data = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes());

            StringBuilder expected = new StringBuilder();
            for (byte b : hmacBytes) {
                expected.append(String.format("%02x", b));
            }

            boolean valid = expected.toString().equals(signatureHeader);
            if (!valid) {
                log.warn("Webhook signature mismatch: expected={}, received={}", expected, signatureHeader);
            }
            return valid;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }

    public boolean verifyApiKey(String apiKey) {
        if (webhookApiKey == null || webhookApiKey.isBlank()) {
            log.warn("Webhook API key not configured, skipping verification");
            return true;
        }
        return webhookApiKey.equals(apiKey);
    }
}
