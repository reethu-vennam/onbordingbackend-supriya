package com.sabbpe.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SmsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.msg91.auth-key:}")
    private String authKey;

    public Map<String, Object> sendMerchantInvite(String mobileNumber, String inviteLink,
                                                   String distributorName, String merchantName) {
        if (authKey == null || authKey.isBlank()) {
            log.warn("MSG91 AUTH_KEY not configured, skipping SMS");
            return Map.of("success", false, "error", "MSG91 not configured");
        }

        String formattedMobile = mobileNumber.startsWith("91") ? mobileNumber : "91" + mobileNumber;
        String smsFlowUrl = "https://control.msg91.com/api/v5/flow";
        String templateId = "694e236ec594fc01ba61af73";

        try {
            Map<String, Object> payload = Map.of(
                    "template_id", templateId,
                    "short_url", "1",
                    "short_url_expiry", "86400",
                    "realTimeResponse", "1",
                    "recipients", List.of(Map.of(
                            "mobiles", formattedMobile,
                            "VAR1", inviteLink,
                            "VAR2", distributorName != null ? distributorName : "SabbPe"
                    ))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("accept", "application/json");
            headers.set("authkey", authKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    smsFlowUrl, HttpMethod.POST, entity, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String requestId = response.getBody().containsKey("request_id")
                        ? (String) response.getBody().get("request_id")
                        : (String) response.getBody().get("message_id");
                log.info("SMS sent to {}: requestId={}", formattedMobile, requestId);
                return Map.of("success", true, "messageId", requestId != null ? requestId : "");
            } else {
                log.warn("SMS API returned {}: {}", response.getStatusCode(), response.getBody());
                return Map.of("success", false, "error",
                        response.getBody() != null ? response.getBody().toString() : "Unknown error");
            }
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", formattedMobile, e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
