package com.sabbpe.service;

import com.sabbpe.dto.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class WhatsAppService {

    @Value("${app.msg91.auth-key}")
    private String authKey;

    @Value("${app.msg91.whatsapp-url}")
    private String whatsappUrl;

    @Value("${app.msg91.integrated-number}")
    private String integratedNumber;

    private final RestTemplate restTemplate;

    public WhatsAppService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public SendResult sendOtp(String to, String templateName, String namespace, String otp) {
        if (authKey == null || authKey.isBlank()) {
            log.error("MSG91 auth key not configured");
            return new SendResult(false, "MSG91 auth key not configured", null);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", authKey);

            Map<String, Object> components = new LinkedHashMap<>();
            components.put("body_1", Map.of("type", "text", "value", otp));
            components.put("button_1", Map.of("subtype", "url", "type", "text", "value", otp));

            Map<String, Object> toAndComponent = new LinkedHashMap<>();
            toAndComponent.put("to", List.of(to));
            toAndComponent.put("components", components);

            Map<String, Object> template = new LinkedHashMap<>();
            template.put("name", templateName);
            template.put("language", Map.of("code", "en", "policy", "deterministic"));
            if (namespace != null && !namespace.isBlank()) {
                template.put("namespace", namespace);
            }
            template.put("to_and_components", List.of(toAndComponent));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("type", "template");
            payload.put("template", template);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("integrated_number", integratedNumber);
            body.put("content_type", "template");
            body.put("payload", payload);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            log.info("Sending WhatsApp OTP to {} via MSG91, template: {}", to, templateName);
            log.debug("MSG91 request body: {}", body);

            ResponseEntity<String> response = restTemplate.postForEntity(whatsappUrl, request, String.class);

            log.info("MSG91 response status: {}, body: {}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WhatsApp OTP sent successfully to {}", to);
                Map<String, Object> status = new LinkedHashMap<>();
                status.put("statusCode", response.getStatusCode().value());
                status.put("data", response.getBody());
                return new SendResult(true, null, status);
            }

            log.error("MSG91 returned status {}: {}", response.getStatusCode(), response.getBody());
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("statusCode", response.getStatusCode().value());
            status.put("data", response.getBody());
            return new SendResult(false, "MSG91 request failed: " + response.getStatusCode(), status);

        } catch (Exception e) {
            log.error("Failed to send WhatsApp OTP to {}: {}", to, e.getMessage(), e);
            return new SendResult(false, e.getMessage(), null);
        }
    }
}
